package com.github.inet.common.protobuf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.util.function.Function;
import java.util.function.Supplier;


public class ProtoBufJsonInterchange<M extends Message, B extends Message.Builder> {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Supplier<B> _builderSupplier;
  private final Function<B, M> _builderToTypeConverter;

  @SuppressWarnings("unchecked")
  public ProtoBufJsonInterchange(Supplier<B> builderSupplier) {
    this(builderSupplier, b -> (M) b.build());
  }

  public ProtoBufJsonInterchange(Supplier<B> builderSupplier, Function<B, M> builderToTypeConverter) {
    _builderSupplier = builderSupplier;
    _builderToTypeConverter = builderToTypeConverter;
  }

  public M convert(ObjectNode json) {
    B builder = _builderSupplier.get();
    try {
      // TODO: explore going directly from JsonNode to M
      JsonFormat.parser().ignoringUnknownFields().merge(OBJECT_MAPPER.writeValueAsString(json), builder);
      return _builderToTypeConverter.apply(builder);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public ObjectNode convert(M message) {
    try {
      // TODO: explore going directly from M to JsonNode
      return OBJECT_MAPPER.readValue(JsonFormat.printer().omittingInsignificantWhitespace().print(message),
          ObjectNode.class);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
