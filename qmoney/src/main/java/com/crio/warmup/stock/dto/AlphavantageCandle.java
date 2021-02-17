package com.crio.warmup.stock.dto;

import java.time.LocalDate;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

// TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
//  Implement the Candle interface in such a way that it matches the parameters returned
//  inside Json response from Alphavantage service.

// Reference - https:www.baeldung.com/jackson-ignore-properties-on-serialization
// Reference - https:www.baeldung.com/jackson-name-of-property
public class AlphavantageCandle implements Candle {
  @JsonProperty("1. open")
  private Double open;
  @JsonProperty("4. close")
  private Double close;
  @JsonProperty("2. high")
  private Double high;
  @JsonProperty("3. low")
  private Double low;
  private LocalDate date;
  
  @JsonProperty("5. volume")
  private Double volume;

  @Override
  public Double getOpen() {
    // TODO Auto-generated method stub
    return this.open;
  }

  @Override
  public Double getClose() {
    // TODO Auto-generated method stub
    return this.close;
  }

  @Override
  public Double getHigh() {
    // TODO Auto-generated method stub
    return this.high;
  }

  @Override
  public Double getLow() {
    // TODO Auto-generated method stub
    return this.low;
  }

  @Override
  public LocalDate getDate() {
    // TODO Auto-generated method stub
    return this.date;
  }

  
  public void setDate(LocalDate value) {
    this.date = value;
  }

  public void setOpen(Double value) {
    this.open = value;
  }

  public void setClose(Double value) {
    this.close = value;
  }

  public void setHigh(Double value) {
    this.high = value;
  }

  public void setLow(Double value) {
    this.low = value;
  }
}

