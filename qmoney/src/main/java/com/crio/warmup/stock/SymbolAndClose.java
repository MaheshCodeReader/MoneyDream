package com.crio.warmup.stock;

public class SymbolAndClose {
  String symbol;
  Double close;


  public SymbolAndClose(String symbol, Double close) {
    this.symbol = symbol;
    this.close = close;
  }

  public String getSymbol() {
    return symbol;
  }

  public Double getClose() {
    return close;
  }

  public void setSymbol(String value) {
    this.symbol = value;
  }

  public void setClose(Double value) {
    this.close = value;
  }
}