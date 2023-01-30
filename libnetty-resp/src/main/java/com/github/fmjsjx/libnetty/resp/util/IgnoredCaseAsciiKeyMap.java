package com.github.fmjsjx.libnetty.resp.util;

import java.util.Arrays;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.collection.ByteObjectHashMap;

/**
 * A simple map like object.
 * <p>
 * The keys stored in this map will always convert to {@link AsciiString}s. All
 * key searching operations will always ignore case considerations.
 * 
 * @param <V> the type of the values
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public final class IgnoredCaseAsciiKeyMap<V> {

    private static final byte VALUE = 0;

    private static final byte CASE_OFFSET = 'a' - 'A';

    private static final byte NODE = 0;
    private static final byte SINGLE_VALUE = 1;

    private final Node root;

    private final int minDepth;

    private int maxKeyLength;

    /**
     * Constructs a new {@link IgnoredCaseAsciiKeyMap}.
     */
    public IgnoredCaseAsciiKeyMap() {
        this(0);
    }

    /**
     * Constructs a new {@link IgnoredCaseAsciiKeyMap} with the specified
     * {@code minDepth}.
     * 
     * @param minDepth the minimum depth, the default is {@code 0}
     */
    public IgnoredCaseAsciiKeyMap(int minDepth) {
        this(new RootNode(), minDepth, 0);
    }

    private IgnoredCaseAsciiKeyMap(Node root, int minDepth, int maxKeyLength) {
        this.root = root;
        this.minDepth = minDepth;
        this.maxKeyLength = maxKeyLength;
    }

    private interface Node {

        Object get(byte key);

        Object put(byte key, Object value);

        Object remove(byte key);

    }

    private static final class RootNode implements Node {

        private final Object[] values = new Object[128];

        @Override
        public Object get(byte key) {
            return values[key];
        }

        @Override
        public Object put(byte key, Object value) {
            Object[] values = this.values;
            Object old = values[key];
            values[key] = value;
            return old;
        }

        @Override
        public Object remove(byte key) {
            Object[] values = this.values;
            Object old = values[key];
            values[key] = null;
            return old;
        }

        private static final String keyToString(byte key) {
            return Character.toString((char) key);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            Object[] values = this.values;
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value != null) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(keyToString((byte) i)).append('=').append(value == this ? "(this Map)" : value);
                    first = false;
                }
            }
            return sb.append('}').toString();
        }

    }

    private static final class NodeImpl extends ByteObjectHashMap<Object> implements Node {

        private NodeImpl() {
        }

        private NodeImpl(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        protected String keyToString(byte key) {
            if (key == 0) {
                return "value";
            }
            return Character.toString((char) key);
        }
    }

    private static final class SingleValue<V> {

        private static final byte[] empty = new byte[0];

        private final byte[] remainingKeyBytes;
        private V value;

        private SingleValue(byte[] remainingKeyBytes, V value) {
            this.remainingKeyBytes = remainingKeyBytes;
            this.value = value;
        }

        private SingleValue(V value) {
            this(empty, value);
        }

        private SingleValue<V> next() {
            if (remainingKeyBytes.length == 1) {
                return new SingleValue<>(value);
            } else {
                byte[] nextKeyBytes = Arrays.copyOfRange(remainingKeyBytes, 1, remainingKeyBytes.length);
                return new SingleValue<>(nextKeyBytes, value);
            }
        }

        @Override
        public String toString() {
            return "SV(" + new String(remainingKeyBytes) + "," + value + ")";
        }

    }

    /**
     * Put a key-value pair.
     * 
     * @param key   the key
     * @param value the value
     * @return the old value if persistent, {@code null} otherwise
     */
    public V put(String key, V value) {
        return put(AsciiString.of(key), value);
    }

    /**
     * Put a key-value pair.
     * 
     * @param key   the key
     * @param value the value
     * @return the old value if persistent, {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public V put(AsciiString key, V value) {
        byte[] array = key.toUpperCase().array();
        int minDepth = this.minDepth;
        maxKeyLength = Math.max(maxKeyLength, array.length);
        Node cur = root;
        for (int i = 0; i < array.length; i++) {
            byte b = array[i];
            if (b >= 'a' && b <= 'z') { // ignore case
                b -= CASE_OFFSET;
            }
            Object next = cur.get(b);
            if (next == null) {
                if (i < minDepth) {
                    NodeImpl node = new NodeImpl();
                    cur.put(b, node);
                    cur = node;
                } else {
                    int remainingLength = array.length - i - 1; // -1 is skip current byte
                    if (remainingLength == 0) {
                        SingleValue<V> sv = new SingleValue<>(value);
                        cur.put(b, sv);
                    } else {
                        byte[] remainingKeyBytes = Arrays.copyOfRange(array, i + 1, array.length);
                        SingleValue<V> sv = new SingleValue<>(remainingKeyBytes, value);
                        cur.put(b, sv);
                    }
                    return null;
                }
            } else if (next instanceof SingleValue) {
                SingleValue<V> sv = (SingleValue<V>) next;
                int remainingKeyLength = sv.remainingKeyBytes.length;
                if (remainingKeyLength == array.length - i - 1) {
                    if (remainingKeyLength == 0
                            || equals(sv.remainingKeyBytes, 0, remainingKeyLength, array, i + 1, remainingKeyLength)) {
                        V old = sv.value;
                        sv.value = value;
                        return old;
                    }
                }
                NodeImpl node;
                if (remainingKeyLength == 0) {
                    node = new NodeImpl();
                    node.put(VALUE, sv.value);
                } else {
                    byte nextByte = sv.remainingKeyBytes[0];
                    if (i == array.length - 1) {
                        node = new NodeImpl();
                        node.put(VALUE, value);
                        node.put(nextByte, sv.next());
                        cur.put(b, node);
                        return null;
                    } else {
                        if (nextByte == array[i + 1]) {
                            node = new NodeImpl(4);
                        } else {
                            node = new NodeImpl();
                        }
                        node.put(nextByte, sv.next());
                    }
                }
                cur.put(b, node);
                cur = node;
            } else {
                cur = (Node) next;
            }
        }
        Object old = cur.put(VALUE, value);
        if (old != null) {
            return (V) old;
        }
        return null;
    }

    private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

    /**
     * Check whether the part of some byte array is "equal to" another one
     *
     * @param a          the first array
     * @param aFromIndex the fromIndex of the first array
     * @param aToIndex   the toIndex of the first array
     * @param b          the second array
     * @param bFromIndex the fromIndex of the second array
     * @param bToIndex   the toIndex of the second array
     * @return {@code true} if the part of some byte array is "equal to" another one, {@code false} otherwise
     */
    public static boolean equals(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        int offset = bFromIndex - aFromIndex;
        for (int i = aFromIndex; i < aToIndex; i++) {
            if (a[i] != b[i + offset]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Search and returns the value by the specified key.
     * 
     * @param key the key
     * @return the value if persistent, {@code null} if not found
     */
    public V get(AsciiString key) {
        return get(Unpooled.wrappedBuffer(key.array()));
    }

    /**
     * Find the value by the specified key.
     * 
     * @param key the key
     * @return an {@code Optional<V>}
     * 
     * @since 1.2
     */
    public Optional<V> find(AsciiString key) {
        return Optional.ofNullable(get(key));
    }

    /**
     * Search and returns the value by the specified key.
     * 
     * @param key the key
     * @return the value if persistent, {@code null} if not found
     */
    @SuppressWarnings("unchecked")
    public V get(ByteBuf key) {
        int length = key.readableBytes();
        if (length > maxKeyLength) {
            return null;
        }
        ValueFinder p = getFinder();
        int index = key.forEachByte(p);
        if (index == -1) {
            if (p.mode == NODE) {
                return (V) p.cur.get(VALUE);
            } else {
                SingleValue<V> sv = p.sv;
                if (sv != null) {
                    return sv.value;
                }
            }
        }
        return null;
    }

    /**
     * Find the value by the specified key.
     * 
     * @param key the key
     * @return an {@code Optional<V>}
     * 
     * @since 1.2
     */
    public Optional<V> find(ByteBuf key) {
        return Optional.ofNullable(get(key));
    }

    /**
     * Creates and returns a copy of this {@link IgnoredCaseAsciiKeyMap}.
     * 
     * @return a copy of this {@code IgnoredCaseAsciiKeyMap}
     */
    public IgnoredCaseAsciiKeyMap<V> copy() {
        return new IgnoredCaseAsciiKeyMap<>(root, minDepth, maxKeyLength);
    }

    private final ValueFinder vf = new ValueFinder();

    private ValueFinder getFinder() {
        ValueFinder p = vf;
        p.reset();
        return p;
    }

    private final class ValueFinder implements ByteProcessor {

        private Node cur = root;
        private byte mode;
        private SingleValue<V> sv;
        private byte index;

        private void reset() {
            cur = root;
            mode = NODE;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean process(byte value) throws Exception {
            if (value >= 'a' && value <= 'z') {
                value -= CASE_OFFSET;
            }
            if (mode == NODE) {
                Object next = cur.get(value);
                if (next instanceof SingleValue) {
                    mode = SINGLE_VALUE;
                    sv = (SingleValue<V>) next;
                    index = 0;
                    return true;
                } else {
                    cur = (NodeImpl) next;
                    return cur != null;
                }
            } else {
                if (value == sv.remainingKeyBytes[index]) {
                    index++;
                    return true;
                } else {
                    sv = null;
                    return false;
                }
            }
        }

    }

    @Override
    public String toString() {
        return root.toString();
    }

}
