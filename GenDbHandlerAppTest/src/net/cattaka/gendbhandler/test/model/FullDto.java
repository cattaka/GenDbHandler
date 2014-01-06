
package net.cattaka.gendbhandler.test.model;

import java.util.Date;

import net.cattaka.util.gendbhandler.Attribute;
import net.cattaka.util.gendbhandler.GenDbHandler;

@GenDbHandler(find = {
        // "blobValue",
        "booleanValue", "byteValue", "characterValue", "dateValue", "doubleValue", "floatValue",
        "integerValue", "key", "longValue",
        // "parcelableValue",
        "pBooleanValue", "pByteValue", "pCharValue", "pDoubleValue", "pFloatValue", "pIntValue",
        "pLongValue", "pShortValue",
        // "serializable",
        "shortValue", "stringValue", "tinyEnum"
})
public class FullDto {
    public enum TinyEnum {
        A, B, C
    }

    @Attribute(primaryKey = true, defaultValue = "0")
    private long key;

    private byte[] blobValue;

    private Boolean booleanValue;

    private Byte byteValue;

    private Character characterValue;

    private Date dateValue;

    private Double doubleValue;

    private TinyEnum tinyEnum;

    private Float floatValue;

    private Integer integerValue;

    private Long longValue;

    @Attribute(defaultValue = "false")
    private boolean pBooleanValue;

    @Attribute(defaultValue = "0")
    private byte pByteValue;

    @Attribute(defaultValue = "0")
    private char pCharValue;

    @Attribute(defaultValue = "0")
    private double pDoubleValue;

    @Attribute(defaultValue = "0")
    private float pFloatValue;

    @Attribute(defaultValue = "0")
    private int pIntValue;

    @Attribute(defaultValue = "0")
    private long pLongValue;

    @Attribute(defaultValue = "0")
    private short pShortValue;

    private TinyParcelable parcelableValue;

    private TinySerializable serializable;

    private Short shortValue;

    private String stringValue;

    public byte[] getBlobValue() {
        return blobValue;
    }

    public void setBlobValue(byte[] blobValue) {
        this.blobValue = blobValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Byte getByteValue() {
        return byteValue;
    }

    public void setByteValue(Byte byteValue) {
        this.byteValue = byteValue;
    }

    public Character getCharacterValue() {
        return characterValue;
    }

    public void setCharacterValue(Character characterValue) {
        this.characterValue = characterValue;
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public TinyEnum getTinyEnum() {
        return tinyEnum;
    }

    public void setTinyEnum(TinyEnum tinyEnum) {
        this.tinyEnum = tinyEnum;
    }

    public Float getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(Float floatValue) {
        this.floatValue = floatValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public boolean getPBooleanValue() {
        return pBooleanValue;
    }

    public void setPBooleanValue(boolean pBooleanValue) {
        this.pBooleanValue = pBooleanValue;
    }

    public byte getPByteValue() {
        return pByteValue;
    }

    public void setPByteValue(byte pByteValue) {
        this.pByteValue = pByteValue;
    }

    public char getPCharValue() {
        return pCharValue;
    }

    public void setPCharValue(char pCharValue) {
        this.pCharValue = pCharValue;
    }

    public double getPDoubleValue() {
        return pDoubleValue;
    }

    public void setPDoubleValue(double pDoubleValue) {
        this.pDoubleValue = pDoubleValue;
    }

    public float getPFloatValue() {
        return pFloatValue;
    }

    public void setPFloatValue(float pFloatValue) {
        this.pFloatValue = pFloatValue;
    }

    public int getPIntValue() {
        return pIntValue;
    }

    public void setPIntValue(int pIntValue) {
        this.pIntValue = pIntValue;
    }

    public long getPLongValue() {
        return pLongValue;
    }

    public void setPLongValue(long pLongValue) {
        this.pLongValue = pLongValue;
    }

    public short getPShortValue() {
        return pShortValue;
    }

    public void setPShortValue(short pShortValue) {
        this.pShortValue = pShortValue;
    }

    public TinyParcelable getParcelableValue() {
        return parcelableValue;
    }

    public void setParcelableValue(TinyParcelable parcelableValue) {
        this.parcelableValue = parcelableValue;
    }

    public TinySerializable getSerializable() {
        return serializable;
    }

    public void setSerializable(TinySerializable serializable) {
        this.serializable = serializable;
    }

    public Short getShortValue() {
        return shortValue;
    }

    public void setShortValue(Short shortValue) {
        this.shortValue = shortValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public long getKey() {
        return key;
    }

    public void setKey(long key) {
        this.key = key;
    }

}
