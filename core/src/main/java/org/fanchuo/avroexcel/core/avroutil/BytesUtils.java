package org.fanchuo.avroexcel.core.avroutil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BytesUtils {
  private BytesUtils() {}

  public static String bytesToString(ByteBuffer byteBuffer) {
    ByteBuffer b64 = Base64.getEncoder().encode(byteBuffer);
    int remaining = b64.remaining();
    byte[] bytes = new byte[remaining];
    b64.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static String bytesToString(byte[] bytes) {
    byte[] b64 = Base64.getEncoder().encode(bytes);
    return new String(b64, StandardCharsets.UTF_8);
  }

  public static ByteBuffer stringToBytes(String str) {
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    return Base64.getDecoder().decode(ByteBuffer.wrap(bytes));
  }

  public static byte[] stringToByteArray(String str) {
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    return Base64.getDecoder().decode(bytes);
  }
}
