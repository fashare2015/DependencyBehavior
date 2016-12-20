# CoordinatorLayout 子 View 之间的依赖管理机制 —— 有向无环图
这几天接触了`CoordinatorLayout`和所谓`layout_behavior`，然后进一步理了一遍源码。有两点惊艳到了我，一个是`嵌套滑动机制`，再一个便是`依赖管理机制`。

对于嵌套滑动`NestedScroll`的分析，网上有挺多博客。那本文打算从`依赖管理`的角度来讲一些东西。
<br/>

#效果图
这是一个模仿`java继承关系`的例子，定义了一个`DependencyBehavior`来描述`继承关系`。
即`A extends B`代表`A DependsOn B`，我拖动`B`时，`A`也会跟着动：

![这里写图片描述](http://img.blog.csdn.net/20161220003827139?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvYTE1MzYxNDEzMQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

<br/>

#分析
实现就不说了，只是定义一个`DependencyBehavior`，简单的实现了两个回调：

- layoutDependsOn()
- onDependentViewChanged()

然后，用这个`DependencyBehavior`，把`7个TextView`串起来，即根据`继承关系`建立好依赖。
<br/>

##建模 —— 有向无环图
那`CoordinatorLayout`是怎么管理这么复杂的树形依赖呢？事实上，依赖关系可能更复杂，比如说我有一个类`MapList`，既实现了`Map`又实现了`List`，此时用树就无法表达它。这个场景是一个有向无环图，`CoordinatorLayout`里正是用这个数据结构来建模的。
<br/>

###图的表示
`mChildDag`是一个`有向无环图`，保存了边(依赖)的信息。其中 `DirectedAcyclicGraph`的实现使用`邻接表`实现的，`邻接表` = `数组`嵌套`链表`，是图的常规表示方式。本质上和`HashMap`的实现差不多，从图的角度看，`HashMap`也是一个`有向无环图`。

```java
public class CoordinatorLayout extends ViewGroup implements NestedScrollingParent {
    private final List<View> mDependencySortedChildren = new ArrayList<>(); // 所有子节点
    private final DirectedAcyclicGraph<View> mChildDag = new DirectedAcyclicGraph<>(); // 有向无环图, 保存了边(依赖)的信息
}
```
<br/>

###建图
`A DependsOn B`可以用一条边表示，代码中方向为`B -> A`，也可以反过来建图，得到的拓扑序相反而已，其他没区别。那我们按照`B -> A`来建一下图：

![这里写图片描述](http://img.blog.csdn.net/20161220074425003?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvYTE1MzYxNDEzMQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
>PS：可以看到，形成了一个树形依赖，其中`Object`的位置决定了所有`TextView`的位置，所以先要确定它。
>其实，箭头反过来更加自然。而`B -> A`建图，方向是反着的，很别扭。其得到的拓扑序列也是反着的。所以代码里会有一步`翻转列表`: 
>Collections.reverse(mDependencySortedChildren);

<br/>
实现的时候，往`mChildDag`里`mChildDag.addEdge(B, A);`即可，他帮你封装了`图`的细节。看一下源码怎么建图：
```java
// 双重 for 循环，简单粗暴，
// 一个 O(N^2) 的算法，还能优化
private void prepareChildren(final boolean forceRefresh) {
    ...
    // 枚举每个 child
    for (int i = 0, count = getChildCount(); i < count; i++) {
        final View view = getChildAt(i);
        final LayoutParams lp = getResolvedLayoutParams(view);

        mChildDag.addNode(view); // 在图中添加节点

        // 枚举每个 otherChild
        for (int j = 0; j < count; j++) {
            if (j == i) 
                continue;
            } 
            final View other = getChildAt(j);
            final LayoutParams otherLp = getResolvedLayoutParams(other);
            if (otherLp.dependsOn(this, other, view)) { // other dependsOn view
                if (!mChildDag.contains(other)) 
                    mChildDag.addNode(other);

                mChildDag.addEdge(view, other); // 添加边 view -> other
            }
        }
    }
    
    // getSortedList() 很关键，这里准备好 mDependencySortedChildren，
    // 之后的 onMeasure(), onLayout() 都直接遍历它即可。
    mDependencySortedChildren.addAll(mChildDag.getSortedList());
    Collections.reverse(mDependencySortedChildren); // 之前建图建反了，这里拓扑序要反一下。
}
```
<br/>

###拓扑排序
前面其实没细讲，为啥要有排序这一步？试想一下，如果不排序，直接`onLayout()`。此时，若`A dependsOn B`且先碰到了`A`，得先知道`B`的位置啊，然而`B`还没`onLayout()`呢。怎么办呢？递归地搞，先搞出`A`的位置，写一个`深度优先搜索`？最开始我是这么想的，想想好复杂。直到看了源码才顿悟，这不就是`拓扑排序`么，实在是被惊艳到了。

`拓扑排序`是`有向无环图`的一种特定排序方式，结果并不唯一，只要保证结果序列中，任意两节点始终满足偏序关系。简单来讲，就是排完序以后，你再`onLayout()`，若`A dependsOn B`且碰到了`A`时，`B`肯定排在`A`前面已经被`onLayout()`过了。恼人的问题解决！！！

此时，再回过头看一下代码：
```java
// 之前的 mChildDag.getSortedList()
// 帮你实现了"拓扑排序"
final class DirectedAcyclicGraph<T> {
    /**
     * Returns a topologically sorted list of the nodes in this graph. This uses the DFS algorithm
     * as described by Cormen et al. (2001). 
     */
    ArrayList<T> getSortedList() {
        mSortResult.clear();
        mSortTmpMarked.clear();

        // Start a DFS from each node in the graph
        // 深度优先搜索，求一个拓扑排序，具体不展开了
        for (int i = 0, size = mGraph.size(); i < size; i++) 
            dfs(mGraph.keyAt(i), mSortResult, mSortTmpMarked);

        return mSortResult;
    }
}
```

之前的分析是基于`24.2.0`的，再看一下老版本`23.2.1`的做法：
甚至没有建图，很奇葩的用`选择排序`实现`拓扑排序`，效率不是一般的低，怪不得在`24.2.0`被优化掉了。不过思路是一致的，我们对比着看一下：
```java
private void prepareChildren() {
    ... // 前面一样，添加节点到 mDependencySortedChildren
    // 吐糟：为何不用快速排序
    selectionSort(mDependencySortedChildren, mLayoutDependencyComparator); // 选择排序
}

// 重点看这个比较器，描述了两个节点之间的 "偏序关系"，
// 也就是描述了 "谁该排在前面" 先 "onLayout()".
final Comparator<View> mLayoutDependencyComparator = new Comparator<View>() {
    @Override
    public int compare(View lhs, View rhs) {
        if (lhs == rhs) {
            return 0;
        } else if (((LayoutParams) lhs.getLayoutParams()).dependsOn(
                CoordinatorLayout.this, lhs, rhs)) { // r 为 dependency, 得排在前面，需要交换一下 
            return 1;
        } else if (((LayoutParams) rhs.getLayoutParams()).dependsOn(
                CoordinatorLayout.this, rhs, lhs)) { // l 为 dependency, 已经排在前面，不需要交换
            return -1;
        } else {
            return 0;
        }
    }
};
```
<br/>

#总结
以上便是`CoordinatorLayout`的依赖机制，基于`有向无环图`。不过，老司机终究是老司机，建模能力太厉害。虽然是基础数据结构，吾等一下子还想不到呢。
