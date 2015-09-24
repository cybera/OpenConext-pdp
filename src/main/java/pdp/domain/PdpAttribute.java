package pdp.domain;

import java.util.List;
import java.util.Objects;

public class PdpAttribute {

  private String name;
  private String value;

  public PdpAttribute() {
  }

  public PdpAttribute(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PdpAttribute that = (PdpAttribute) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String toString() {
    return "PdpAttribute{" +
        "name='" + name + '\'' +
        ", value='" + value + '\'' +
        '}';
  }
}
