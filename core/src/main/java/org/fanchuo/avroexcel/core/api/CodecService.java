package org.fanchuo.avroexcel.core.api;

import org.fanchuo.avroexcel.core.avroutil.IGenericDataConf;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;

public interface CodecService {
  String getName();

  IDecoderBuilder makeDecoder(IGenericDataConf genericDataConf);

  IEncoderBuilder makeEncoder(IGenericDataConf genericDataConf);
}
