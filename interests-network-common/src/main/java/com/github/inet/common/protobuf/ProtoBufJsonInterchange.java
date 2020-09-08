package com.github.inet.common.protobuf;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.util.function.Function;
import java.util.function.Supplier;


public class ProtoBufJsonInterchange<M extends Message, B extends Message.Builder> {
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

  public M convert(String json) {
    B builder = _builderSupplier.get();
    try {
      JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
      return _builderToTypeConverter.apply(builder);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String convert(M message) {
    try {
      return JsonFormat.printer().omittingInsignificantWhitespace().print(message);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
