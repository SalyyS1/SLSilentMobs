package vn.saly.silentmobs.visibility;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the optional MEG client-mod animation payload without linking against
 * Mojang server classes. The payload is only considered usable when its
 * channel and complete binary structure are both recognized.
 */
final class ModelEngineBulkPayloadInspector {

    private static final String CHANNEL = "modelengine:bulk_data";
    private static final int BULK_PACKET_TYPE = 0;
    private static final int MAX_ENTRIES = 16_384;

    private ModelEngineBulkPayloadInspector() {
    }

    static Inspection inspect(Object payload) {
        if (!isBulkPayload(payload)) {
            return Inspection.notBulk();
        }

        Optional<byte[]> bytes = bytesFrom(payload);
        if (bytes.isEmpty()) {
            return Inspection.unreadable();
        }

        try {
            return Inspection.recognized(readEntityIds(bytes.get()));
        } catch (IllegalArgumentException exception) {
            return Inspection.unreadable();
        }
    }

    private static boolean isBulkPayload(Object payload) {
        if (payload == null) {
            return false;
        }

        Object type = invokeNoArg(payload, "type", "getType");
        Object id = type == null ? invokeNoArg(payload, "id", "getId")
                : invokeNoArg(type, "id", "getId");
        return id != null && CHANNEL.equals(String.valueOf(id));
    }

    private static Optional<byte[]> bytesFrom(Object payload) {
        Object data = invokeNoArg(payload, "data", "getData");
        if (data instanceof byte[] bytes) {
            return Optional.of(bytes.clone());
        }
        if (data instanceof ByteBuffer buffer) {
            ByteBuffer copy = buffer.asReadOnlyBuffer();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            return Optional.of(bytes);
        }
        if (data == null) {
            return Optional.empty();
        }

        try {
            int readerIndex = (int) data.getClass().getMethod("readerIndex").invoke(data);
            int readableBytes = (int) data.getClass().getMethod("readableBytes").invoke(data);
            if (readableBytes < 0) {
                return Optional.empty();
            }
            byte[] bytes = new byte[readableBytes];
            data.getClass().getMethod("getBytes", int.class, byte[].class)
                    .invoke(data, readerIndex, bytes);
            return Optional.of(bytes);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static Object invokeNoArg(Object target, String... names) {
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                return method.invoke(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Different Minecraft and ModelEngine revisions use different accessors.
            }
        }
        return null;
    }

    private static Set<Integer> readEntityIds(byte[] payload) {
        Cursor cursor = new Cursor(payload);
        if (cursor.readUnsignedByte() != BULK_PACKET_TYPE) {
            throw new IllegalArgumentException("Unknown ModelEngine bulk packet type");
        }

        int count = cursor.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid ModelEngine bulk entry count");
        }

        Set<Integer> entityIds = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            entityIds.add(cursor.readVarInt());
            int fields = cursor.readUnsignedByte();
            skipFields(cursor, fields);
        }
        if (!cursor.atEnd()) {
            throw new IllegalArgumentException("Unexpected ModelEngine bulk payload data");
        }
        return Set.copyOf(entityIds);
    }

    private static void skipFields(Cursor cursor, int fields) {
        if ((fields & 0x01) != 0) cursor.skip(6); // translation
        if ((fields & 0x02) != 0) cursor.skip(8); // left rotation
        if ((fields & 0x04) != 0) cursor.skip(6); // scale
        if ((fields & 0x08) != 0) cursor.skip(8); // right rotation
        if ((fields & 0x10) != 0) cursor.readVarInt(); // transform duration
        if ((fields & 0x20) != 0) cursor.skip(5); // glow
        if ((fields & 0x40) != 0) cursor.skip(4); // brightness
        if ((fields & 0x80) != 0) cursor.skip(6); // billboard, range, display type
    }

    record Inspection(boolean bulkPayload, boolean readable, Set<Integer> entityIds) {
        static Inspection notBulk() {
            return new Inspection(false, false, Set.of());
        }

        static Inspection unreadable() {
            return new Inspection(true, false, Set.of());
        }

        static Inspection recognized(Set<Integer> entityIds) {
            return new Inspection(true, true, entityIds);
        }
    }

    private static final class Cursor {
        private final byte[] data;
        private int position;

        private Cursor(byte[] data) {
            this.data = data;
        }

        int readUnsignedByte() {
            if (position >= data.length) {
                throw new IllegalArgumentException("Unexpected end of payload");
            }
            return Byte.toUnsignedInt(data[position++]);
        }

        int readVarInt() {
            int value = 0;
            for (int shift = 0; shift < 35; shift += 7) {
                int next = readUnsignedByte();
                value |= (next & 0x7F) << shift;
                if ((next & 0x80) == 0) {
                    return value;
                }
            }
            throw new IllegalArgumentException("VarInt exceeds five bytes");
        }

        void skip(int count) {
            if (count < 0 || position + count > data.length) {
                throw new IllegalArgumentException("Unexpected end of payload");
            }
            position += count;
        }

        boolean atEnd() {
            return position == data.length;
        }
    }
}
