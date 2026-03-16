package org.fanchuo.avroexcel.core.impl;

import org.fanchuo.avroexcel.core.api.CodecService;
import org.fanchuo.avroexcel.core.avroutil.IGenericDataConf;
import org.fanchuo.avroexcel.core.decoder.AvroDecoderBuilder;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.encoder.AvroEncoderBuilder;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;

public class AvroCodecService implements CodecService {
  @Override
  public String getName() {
    return "AVRO";
  }

  @Override
  public IDecoderBuilder makeDecoder(IGenericDataConf genericDataConf) {
    return new AvroDecoderBuilder(genericDataConf);
  }

  @Override
  public IEncoderBuilder makeEncoder(IGenericDataConf genericDataConf) {
    return new AvroEncoderBuilder(genericDataConf);
  }
}
