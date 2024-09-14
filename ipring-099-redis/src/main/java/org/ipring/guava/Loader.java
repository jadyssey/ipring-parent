package org.ipring.guava;

@FunctionalInterface
public interface Loader<K, V> {
    V load(K s);
}