package util;

import java.io.*;

public class ByteConverter {

    public static byte[] objectToBytes(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] bytes;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            bytes = bos.toByteArray();
        } finally {
            bos.close();
        }
        return bytes;
    }

    public static <T> T bytesToObjects(byte[] bytes) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        T object = null;
        try (ObjectInput in = new ObjectInputStream(bis)) {
            object = (T)in.readObject();
        } catch (ClassNotFoundException e) {
            System.out.println("Class for bytes conversion does not exist!");
        }
        return object;
    }
}
