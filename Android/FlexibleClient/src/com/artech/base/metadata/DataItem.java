package com.artech.base.metadata;

import java.io.Serializable;

import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.enums.ImageUploadModes;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class DataItem extends PropertiesObject implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String mName;
	private boolean mIsCollection;
	private ITypeDefinition mBaseDataType = null;
	private String mControlType = null;
	private int mStorageType;

	// Cached properties (most accessed, or slowest).
	private Boolean mIsEnumeration;
	private String mInputPicture;

	public DataItem(ITypeDefinition attribute)
	{
		mBaseDataType = attribute;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public boolean isVariable() { return false; }

	protected void setDataType(ITypeDefinition def) { mBaseDataType = def; }

	public ITypeDefinition getBaseType() { return mBaseDataType; }

	public <TType extends ITypeDefinition> TType getTypeInfo(Class<TType> tType)
	{
		// Go up the chain of base types until encountering the specified type, or not having any more.
		// Might be an indirect inheritance by way of a Domain.
		ITypeDefinition type = getBaseType();
		while (type != null && !(tType.isInstance(type)))
			type = type.getBaseType();

		return (type != null ? tType.cast(type) : null);
	}

	public boolean isCollection()
	{
		return mIsCollection;
	}

	void setIsCollection(boolean value)
	{
		mIsCollection = value;
	}

	public String getName()
	{
		if (mName == null)
			mName = (String)getProperty("Name"); //$NON-NLS-1$

		return mName;
	}

	public String getCaption()
	{
		return Services.Resources.getTranslation(optStringProperty("Caption")); //$NON-NLS-1$
	}

	public void setName(String name)
	{
		setProperty("Name", name); //$NON-NLS-1$
		mName = name;
	}

	@Override
	public Object getProperty(String propName)
	{
		Object localValue = super.getProperty(propName);

		if (localValue == null && mBaseDataType != null) // We don't have a value for this property so inherit from based on type
			localValue = mBaseDataType.getProperty(propName);

		return localValue;
	}

	public String getType()
	{
		if (mBaseDataType != null)
			return mBaseDataType.getType();

		return Strings.EMPTY;
	}

	public String getInputPicture()
	{
		if (mInputPicture == null)
			mInputPicture = super.optStringProperty("InputPicture"); //$NON-NLS-1$

		return mInputPicture;
	}

	public int getLength() {
		return super.optIntProperty("Length"); //$NON-NLS-1$
	}

	public int getDecimals() {
		return super.optIntProperty("Decimals"); //$NON-NLS-1$
	}

	public boolean getSigned() {
		return super.optBooleanProperty("Signed"); //$NON-NLS-1$
	}

	public boolean getReadOnly() {
		return super.optBooleanProperty("ReadOnly"); //$NON-NLS-1$
	}

	public boolean getAutoNumber() {
		return super.optBooleanProperty("AutoNumber"); //$NON-NLS-1$
	}

	public boolean getIsEnumeration()
	{
		if (mIsEnumeration == null)
			mIsEnumeration = super.optBooleanProperty("IsEnumeration"); //$NON-NLS-1$

		return mIsEnumeration;
	}

	public String getEnumerationType() {
		return super.optStringProperty("EnumerationType"); //$NON-NLS-1$
	}

	public boolean IsDescription()
	{
		return super.optBooleanProperty("DescriptionAtt"); //$NON-NLS-1$
	}

	private DataTypeName mDataTypeName;

	public DataTypeName getDataTypeName() {
		if (mDataTypeName == null) {
			String dataName  = getType();
			ITypeDefinition parent = getBaseType();
			while (parent != null) {
				if (parent instanceof DomainDefinition) {
					if (DomainDefinition.isSpecialDomain(parent))
					{
						dataName = parent.getName();
						break;
					}
				}
				parent = parent.getBaseType();
			}
			mDataTypeName = new DataTypeName(dataName);
		}

		return mDataTypeName;
	}

	public boolean isKey() {
		return super.optBooleanProperty("IsKey"); //$NON-NLS-1$
	}

	public String getControlType()
	{
		if (mControlType != null)
			return mControlType;

		mControlType = getDataTypeName().GetControlType();

		// If different from default use the domain control type.
		if (mControlType != null && !mControlType.equalsIgnoreCase(ControlTypes.TextBox))
			return mControlType;

		//Check if is enumeration
		if (getIsEnumeration())// && getEnumerationType().length()>0)
		{
			mControlType = ControlTypes.EnumCombo;
			return mControlType;
		}

		//If not calculate it from m_Type
		String type = getType(); // Not m_Type, since it may be overriden by variable definition.
		mControlType = ControlTypes.TextBox;//Default TextBox.

		if (type.equals("int") || type.equals("numeric")) //$NON-NLS-1$ //$NON-NLS-2$
			mControlType = ControlTypes.NumericTextBox;
		else if (type.equals(DataTypes.date) || type.equals(DataTypes.dtime) || type.equals(DataTypes.time) || type.equals(DataTypes.datetime))
			mControlType = ControlTypes.DateBox;
		else // This should be changed on metadata writers so all these cases come with value "image"
			if (type.equalsIgnoreCase("bits") || type.equalsIgnoreCase("bitmap")) //$NON-NLS-1$ //$NON-NLS-2$
				mControlType = ControlTypes.PhotoEditor;
		else if (type.equalsIgnoreCase("binary"))
				mControlType = ControlTypes.BinaryBlob;
		return mControlType;
	}

	public DataItem getCopy()
	{
		DataItem item = new DataItem(getBaseType());
		item.setInternalProperties(cloneProperties());
		return item;
	}

	public void merge(DataItem item)
	{
		getInternalProperties().putAll(item.getInternalProperties());
	}

	public Object getEmptyValue()
	{
		if (mBaseDataType != null)
			return mBaseDataType.getEmptyValue(isCollection());
		else
			return null;
	}

	public boolean isEmptyValue(Object value)
	{
		if (mBaseDataType != null)
			return mBaseDataType.isEmptyValue(value);
		else
			return (value == null || value.toString().length() == 0);
	}

	public int getStorageType() { return mStorageType; }
	protected void setStorageType(int value) { mStorageType = value; }

	public int getMaximumUploadSizeMode()
	{
		String maxUploadSize = super.optStringProperty("MaximumUploadSize");
		if (maxUploadSize.equalsIgnoreCase("small"))
			return ImageUploadModes.SMALL;
		else if (maxUploadSize.equalsIgnoreCase("medium"))
			return ImageUploadModes.MEDIUM;
		else if (maxUploadSize.equalsIgnoreCase("actualsize") || maxUploadSize.equalsIgnoreCase("actual"))
			return ImageUploadModes.ACTUALSIZE;
		//default
		return ImageUploadModes.LARGE;
	}

	public boolean isMediaOrBlob()
	{
		String controlType = getControlType();
		if (controlType != null)
		{
			return (controlType.equals(ControlTypes.PhotoEditor) ||
					controlType.equals(ControlTypes.AudioView) ||
					controlType.equals(ControlTypes.VideoView) ||
					controlType.equals(ControlTypes.BinaryBlob) );
		}
		else
			return false;
	}
}
