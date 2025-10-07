package org.mksn.inintobot.gcp.store

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LRUCache<K, V>(private val maxSize: Int) {
    private val cache = ConcurrentHashMap<K, Node<K, V>>()
    private val lock = ReentrantReadWriteLock()
    private var head: Node<K, V>? = null
    private var tail: Node<K, V>? = null

    private data class Node<K, V>(
        val key: K,
        var value: V,
        var prev: Node<K, V>? = null,
        var next: Node<K, V>? = null
    )

    fun get(key: K): V? = lock.read {
        cache[key]?.let { node ->
            lock.write {
                moveToHead(node)
                node.value
            }
        }
    }

    fun put(key: K, value: V) = lock.write {
        cache[key]?.let { node ->
            node.value = value
            moveToHead(node)
        } ?: run {
            val newNode = Node(key, value)
            cache[key] = newNode
            addToHead(newNode)

            if (cache.size > maxSize) {
                tail?.let { removeTail() }
            }
        }
    }

    fun remove(key: K) = lock.write {
        cache.remove(key)?.let { node ->
            removeNode(node)
        }
    }

    fun clear() = lock.write {
        cache.clear()
        head = null
        tail = null
    }

    private fun moveToHead(node: Node<K, V>) {
        removeNode(node)
        addToHead(node)
    }

    private fun addToHead(node: Node<K, V>) {
        node.prev = null
        node.next = head
        head?.prev = node
        head = node
        if (tail == null) tail = node
    }

    private fun removeNode(node: Node<K, V>) {
        node.prev?.next = node.next
        node.next?.prev = node.prev
        if (head == node) head = node.next
        if (tail == node) tail = node.prev
    }

    private fun removeTail() {
        tail?.let { node ->
            cache.remove(node.key)
            removeNode(node)
        }
    }
}
