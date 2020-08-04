/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.jgea.representation.tree;

import it.units.malelab.jgea.core.util.Sized;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author eric
 */
public class Tree<C> implements Serializable, Sized, Iterable<Tree<C>> {

  private final C content;
  private final List<Tree<C>> children = new ArrayList<>();
  private Tree<C> parent;

  public static <K> Tree<K> copyOf(Tree<K> other) {
    Tree<K> t = new Tree<>(other.content, null);
    for (Tree<K> child : other.children) {
      t.addChild(Tree.copyOf(child));
    }
    return t;
  }

  public static <K> Tree<K> of(K content, Tree<K>... children) {
    Tree<K> t = new Tree<>(content, null);
    for (Tree<K> child : children) {
      t.addChild(child);
    }
    return t;
  }

  public static <K, H> Tree<H> map(Tree<K> other, Function<K, H> mapper) {
    Tree<H> t = Tree.of(mapper.apply(other.content));
    for (Tree<K> child : other.children) {
      t.addChild(Tree.map(child, mapper));
    }
    return t;
  }

  private Tree(C content, Tree<C> parent) {
    this.content = content;
    this.parent = parent;
  }

  public C content() {
    return content;
  }

  public void addChild(Tree<C> child) {
    children.add(child);
    child.parent = this;
  }

  public boolean removeChild(Tree<C> child) {
    return children.remove(child);
  }

  public void clearChildren() {
    children.clear();
  }

  public Tree<C> child(int i) {
    return children.get(i);
  }

  public boolean isLeaf() {
    return children.isEmpty();
  }

  public int nChildren() {
    return children.size();
  }

  public int height() {
    return 1 + children.stream().mapToInt(Tree::height).max().orElse(0);
  }

  public int depth() {
    if (parent == null) {
      return 0;
    }
    return parent.depth() + 1;
  }

  public List<C> visitDepth() {
    List<C> contents = new ArrayList<>(1 + children.size());
    contents.add(content);
    children.forEach(c -> contents.addAll(c.visitDepth()));
    return contents;
  }

  public List<C> visitLeaves() {
    return leaves().stream().map(Tree::content).collect(Collectors.toList());
  }

  public List<Tree<C>> leaves() {
    if (children.isEmpty()) {
      return List.of(this);
    }
    List<Tree<C>> leaves = new ArrayList<>();
    children.forEach(c -> leaves.addAll(c.leaves()));
    return leaves;
  }

  public List<Tree<C>> topSubtrees() {
    List<Tree<C>> subtrees = new ArrayList<>();
    subtrees.add(this);
    children.forEach(c -> subtrees.addAll(c.topSubtrees()));
    return subtrees;
  }

  @Override
  public Iterator<Tree<C>> iterator() {
    return children.iterator();
  }

  @Override
  public int size() {
    return 1 + children.stream().mapToInt(Tree::size).sum();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Tree<?> tree = (Tree<?>) o;
    return Objects.equals(content, tree.content) &&
        children.equals(tree.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, children);
  }

  @Override
  public String toString() {
    String s = content.toString() +
        (children.isEmpty() ? "" : ("[" + children.stream().map(Tree::toString).collect(Collectors.joining(",")) + "]"));
    return s;
  }
}