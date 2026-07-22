package vn.saly.silentmobs.visibility;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelEngineBulkPayloadInspectorTest {

    @Test
    void readsDisplayEntityIdsFromMegBulkPayload() {
        byte[] payload = payload(entry(37, 0x01, new byte[6]), entry(42, 0x30, new byte[6]));

        ModelEngineBulkPayloadInspector.Inspection inspection = ModelEngineBulkPayloadInspector.inspect(
                new TestPayload(new TestType("modelengine:bulk_data"), payload));

        assertTrue(inspection.bulkPayload());
        assertTrue(inspection.readable());
        assertEquals(Set.of(37, 42), inspection.entityIds());
    }

    @Test
    void rejectsIncompleteOrDifferentPayloads() {
        ModelEngineBulkPayloadInspector.Inspection malformed = ModelEngineBulkPayloadInspector.inspect(
                new TestPayload(new TestType("modelengine:bulk_data"), new byte[] {0, 1, 42}));
        ModelEngineBulkPayloadInspector.Inspection unrelated = ModelEngineBulkPayloadInspector.inspect(
                new TestPayload(new TestType("example:other"), new byte[] {0}));

        assertTrue(malformed.bulkPayload());
        assertFalse(malformed.readable());
        assertFalse(unrelated.bulkPayload());
    }

    private static byte[] payload(byte[]... entries) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0);
        writeVarInt(output, entries.length);
        for (byte[] entry : entries) {
            output.writeBytes(entry);
        }
        return output.toByteArray();
    }

    private static byte[] entry(int entityId, int fields, byte[] fieldData) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeVarInt(output, entityId);
        output.write(fields);
        output.writeBytes(fieldData);
        return output.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream output, int value) {
        while ((value & ~0x7F) != 0) {
            output.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.write(value);
    }

    private record TestPayload(TestType type, byte[] data) {
    }

    private record TestType(String id) {
    }
}
