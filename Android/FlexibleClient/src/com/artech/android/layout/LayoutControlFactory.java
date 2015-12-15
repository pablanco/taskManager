package com.artech.android.layout;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.artech.R;
import com.artech.android.layout.SectionsLayoutVisitor.LayoutSection;
import com.artech.base.metadata.DataTypeName;
import com.artech.base.metadata.DetailDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.Properties;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.enums.LayoutModes;
import com.artech.base.metadata.layout.CellDefinition;
import com.artech.base.metadata.layout.ComponentDefinition;
import com.artech.base.metadata.layout.ContentDefinition;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.ILayoutContainer;
import com.artech.base.metadata.layout.LayoutActionDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.TabControlDefinition;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.rules.PromptRuleDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.ImageHelper;
import com.artech.common.LayoutHelper;
import com.artech.common.PhoneHelper;
import com.artech.common.PromptHelper;
import com.artech.common.StandardImages;
import com.artech.common.TrnHelper;
import com.artech.controls.DataBoundControl;
import com.artech.controls.DynamicSpinnerControl;
import com.artech.controls.GxAudioView;
import com.artech.controls.GxButton;
import com.artech.controls.GxImageViewData;
import com.artech.controls.GxImageViewStatic;
import com.artech.controls.GxSectionLink;
import com.artech.controls.GxTableLayout;
import com.artech.controls.GxTextBlockTextView;
import com.artech.controls.GxTextView;
import com.artech.controls.GxVideoView;
import com.artech.controls.GxWebView;
import com.artech.controls.IGxEdit;
import com.artech.controls.IHandleSemanticDomain;
import com.artech.controls.RatingControl;
import com.artech.controls.tabs.GxTabControl;
import com.artech.fragments.ComponentContainer;
import com.artech.fragments.GridContainer;
import com.artech.ui.Coordinator;
import com.artech.ui.test.ControlTestHelper;
import com.artech.usercontrols.IGxUserControl;
import com.artech.usercontrols.UcFactory;
import com.artech.utils.Cast;
import com.fedorvlasov.lazylist.ImageLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class LayoutControlFactory
{
	/**
	 * Creates an Android view for a layout item definition.
	 */
	public static View createView(Context context, Coordinator coordinator, LayoutItemDefinition item, ImageLoader loader, short layoutMode, short trnMode, boolean addDomainActions)
	{
		// Create the view.
		View view = createViewInternal(context, coordinator, item, loader, layoutMode, trnMode, addDomainActions);

		// Set special tags for later lookup.
		setDefinition(view, item);

		// Set properties that are common to all views.
		setGenericProperties(view, item);

		// Set event handlers.
		setImplicitTapHandlers(view, item, layoutMode, trnMode);

		return view;
	}

	public static boolean setDefinition(View view, LayoutItemDefinition definition)
	{
		if (view != null && definition != null)
		{
			view.setTag(LayoutTag.CONTROL_NAME, definition.getName());
			view.setTag(LayoutTag.CONTROL_DEFINITION, definition);

			// Workaround, consider a special case for DataBoundcontrols. So data items controls can know their definition.
			DataBoundControl dbControl = Cast.as(DataBoundControl.class, view);
			if (dbControl != null)
			{
				setDefinition((View) dbControl.getEdit(), definition);
			}
			else
			{
				ControlTestHelper.onGxControlCreated(view, definition);
			}

			return true;
		}
		else
			return false;
	}

	private static void setGenericProperties(View view, LayoutItemDefinition definition)
	{
		if (!(view instanceof DataBoundControl))
			view.setTag(definition.getName()); // Data controls have a special tag.

		view.setVisibility(definition.isVisible() ? View.VISIBLE : View.INVISIBLE);

		// Enabled is only set when FALSE. Otherwise some controls (such as Checkbox) become
		// editable when enabled, disregarding the "ReadOnly" property.
		boolean enabled = definition.isEnabled();
		if (!enabled)
			view.setEnabled(definition.isEnabled());
	}

	private static void setImplicitTapHandlers(View view, LayoutItemDefinition definition, short layoutMode, short trnMode)
	{
		PromptRuleDefinition promptInfo = definition.getPrompt(layoutMode, trnMode);
		if (promptInfo != null)
			PromptHelper.setAssociatedPrompt(view, promptInfo);
	}

	// create view for layout in View and List forms absolute layouts.
	private static View createViewInternal(Context context, Coordinator coordinator, LayoutItemDefinition item, ImageLoader loader, short layoutMode, short trnMode, boolean addDomainActions)
	{
		String itemTypeName = item.getType();
		if (item instanceof LayoutActionDefinition)
		{
			LayoutActionDefinition layoutAction = (LayoutActionDefinition)item;
			GxButton button = new GxButton(context, coordinator, layoutAction);
			setDefinition(button, item);
			GxTheme.applyStyle(button, layoutAction.getThemeClass());
			return button;
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Data))
		{
			View view = createDataBoundView(context, coordinator, item, loader, layoutMode, trnMode, addDomainActions);
			((IGxEdit)view).setGx_Tag(item.getDataId());
			return view;
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Group))
		{
			ILayoutContainer itemContainer = (ILayoutContainer) item;
			TableDefinition def = itemContainer.getContent();

			GxLayout group = new GxLayout(context, def, coordinator);
			setDefinition(group, item);
			GxTheme.applyStyle(group, item.getThemeClass());
			return group;
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Image))
		{
			GxImageViewStatic image = new GxImageViewStatic(context, item);
			image.setAlignment(item.CellGravity);
			String imageName = MetadataLoader.getObjectName(item.optStringProperty("@image")); //$NON-NLS-1$
			setDefinition(image, item);
			GxTheme.applyStyle(image, item.getThemeClass());
			ImageHelper.displayImage(image, imageName);

			return image;
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.UserControl))
		{
			View ucView = createUserControl(context, coordinator, item);
			if (ucView != null)
				return ucView;
			else
				Services.Log.warning("failed to create uc type: " + item.getControlType());
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Tab))
		{
			GxTabControl tabControl = new GxTabControl(context, coordinator, (TabControlDefinition)item);
			setLayoutParams(tabControl);
			return tabControl;
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Table))
		{
			//Temporary , table for ads
			if (isAdsTable(item))
			{
				GxTableLayout table = new GxTableLayout(context);
				setDefinition(table, item);
				GxTheme.applyStyle(table, item.getThemeClass());
				table.setGravity(item.CellGravity);

				AdView adView = LayoutHelper.getAdView((Activity) context);
				adView.setVisibility(item.isVisible() ? View.VISIBLE : View.INVISIBLE);

				// Initiate a generic request to load it with an ad
				AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
				adRequestBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
				AdRequest adRequest = adRequestBuilder.build();
				//adRequest.addKeyword("keyword");
				adView.loadAd(adRequest);

				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				table.addView(adView, params);
				//return adView;
				return table;

			}
			else
			{
				if ((item.getThemeClass() != null && item.getThemeClass().hasMarginSet()) || (item.CellGravity != Alignment.NONE))
				{
					//Table is now a GxTableLayout with a GxLayout inside it for margins and align
					GxTableLayout table = new GxTableLayout(context);
					setDefinition(table, item);
					GxTheme.applyStyle(table, item.getThemeClass());

					// Gravity default seems to be Gravity.Start = 8388611
					// not change it, if not necessary.
					if (item.CellGravity != Alignment.NONE)
					{
						table.setVerticalGravity(item.CellGravity & Alignment.VERTICAL_MASK);
						table.setHorizontalGravity(item.CellGravity & Alignment.HORIZONTAL_MASK);
					}

					return table;
				}
				else
				{
					//Create GxLayout directly to avoid nested controls
					GxLayout table = new GxLayout(context, (TableDefinition) item, coordinator);
					setDefinition(table, item);
					GxTheme.applyStyle(table, item.getThemeClass());
					//table.setGravity(item.CellGravity);
					return table;
				}
			}
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.TextBlock))
		{
			GxTextBlockTextView textBlock = new GxTextBlockTextView(context, item);
			setDefinition(textBlock, item);
			GxTheme.applyStyle(textBlock, item.getThemeClass());
			textBlock.setCaption(item.getCaption());
			textBlock.setGravity(item.CellGravity);
			return textBlock;
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Grid))
		{
			// Create wrapper for a grid control.
			GridContext gridContext = new GridContext(coordinator, context, loader);
			GridContainer gridContainer = new GridContainer(gridContext, coordinator, (GridDefinition)item);
			setDefinition(gridContainer, item);
			GxTheme.applyStyle(gridContainer, item.getThemeClass());
			return gridContainer;
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Component))
		{
			ComponentContainer container = new ComponentContainer(context, (ComponentDefinition)item);
			setDefinition(container, item);
			return container;
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.OneContent))
		{
			ContentDefinition content = (ContentDefinition) item;
			if (content.getDisplayType().equalsIgnoreCase(Properties.ContentDisplayType.LINK_CONTENT))
			{
				LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				GxSectionLink sectionLink = (GxSectionLink) inflater.inflate(R.layout.sectionlinkcontrol, null);
				setDefinition(sectionLink, item);
				sectionLink.setDefinition(content);
				return sectionLink;
			}
			else
			{
				ComponentContainer container = new ComponentContainer(context, content);
				setDefinition(container, item);
				return container;
			}
		}

		if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.AllContent))
		{
			// TODO: Create tab control with inline section inside it.
			//Create tab control here add create it definition.
			IDataViewDefinition dataView = item.getLayout().getParent();
			if (dataView != null && dataView instanceof DetailDefinition)
			{
				List<LayoutSection> tabsSections = LayoutHelper.getDetailSections((DetailDefinition)dataView, trnMode);

				if (tabsSections.size() == 1)
				{
					// Return this section as content
					ContentDefinition itemDef = LayoutHelper.getContentDefinition(item.getLayout(), item.getParent(), tabsSections);
					ComponentContainer container = new ComponentContainer(context, itemDef);
					setDefinition(container, item);
					return container;
				}
				else
				{
					// return a tab control with a section in each tab page
					TabControlDefinition tabDefinition = LayoutHelper.getTabControlDefinition(item.getLayout(), item.getParent(), tabsSections);
					GxTabControl tabControl = new GxTabControl(context, coordinator, tabDefinition);

					//set tab content size
					CellDefinition parentCell = (CellDefinition)item.getParent();
					tabDefinition.calculateBounds(parentCell.getAbsoluteWidth(), parentCell.getAbsoluteHeight());

					return tabControl;
				}
			}
		}



		Services.Log.warning("Unknown layout item type: " + itemTypeName);
		return null;
	}

	private static View createUserControl(Context context, Coordinator coordinator, LayoutItemDefinition layoutItem)
	{
		IGxUserControl uc = UcFactory.createUserControl(context, coordinator, layoutItem);
		if (uc != null)
			return Cast.as(View.class, uc);
		return null;
	}

	private static View createDataBoundView(Context context, Coordinator coordinator, LayoutItemDefinition item, ImageLoader loader, short layoutMode, short trnMode, boolean addDomainActions)
	{
		if (item.getDataItem() == null)
			return new GxTextView(context, item); // Layout data item without data item?

		DataBoundControl layout = new DataBoundControl(context, coordinator, item);
		setDefinition(layout, item);

		// Calculate the read only using the layoutMode and att/var type for "Auto"
		boolean readonly = item.getReadOnly(layoutMode, trnMode);

		IGxEdit attView;
		if (!readonly)
			attView = createAttEditControl(context, coordinator, loader, item, layout);
		else
			attView = createAttViewControl(context, coordinator, loader, item, layout, layoutMode);

		// Apply Style to gx DataBoundControl, label and att inside also.
		GxTheme.applyStyle(layout, item.getThemeClass());
		layout.setLabelAndFieldLayoutNoTheme();

		// Add domain action if exists
		if (addDomainActions && readonly && (layoutMode != LayoutModes.LIST || item.getAutoLink())) //in list only with auto link
			addActionIconToLabelAndFieldLayout(context, item, layout,  attView);

		// Add FK action if exists.
		// Not add for "Dynamic Combo Box", "com.artech.controls.DynamicSpinnerControl"
		if (item.hasPrompt(layoutMode, trnMode) && (!(attView instanceof DynamicSpinnerControl)))
			addPromptIcon(layout, attView);

		return layout;
	}

	private static void setLayoutParams(View edit)
	{
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
		edit.setLayoutParams(lp);
	}

	private static IGxEdit createAttViewControl(Context context, Coordinator coordinator, ImageLoader loader, LayoutItemDefinition item, DataBoundControl layout, short layoutMode)
	{
		//Try to get user Control
		IGxEdit attView;
		IGxEdit userControlView = TrnHelper.getUserControlFromLayoutItem(context, coordinator, item);
		if (userControlView != null)
		{
			attView = userControlView.getViewControl();

			// TODO: Remove this; should be based on properties.
			if (layoutMode == LayoutModes.LIST && attView instanceof RatingControl)
				((RatingControl)attView).prepareForList();

			//set user control as view control
			layout.addEdit((View)attView);
		}
		else
		{
			// Photo Editor
			if (item.getControlType().equals(ControlTypes.PhotoEditor))
			{
				GxImageViewData gxImage = new GxImageViewData(context, item, loader);
				setLayoutParams(gxImage);
				layout.addEdit(gxImage);
				attView = gxImage;
			}
			else if (item.getControlType().equals(ControlTypes.WebView))
			{
				// component/html webview DONE
				String domainDataType = Strings.EMPTY;
				if (item.getDataTypeName() != null)
					domainDataType = item.getDataTypeName().GetDataType();

				if (domainDataType.equals(DataTypes.component) || domainDataType.equals(DataTypes.html))
				{
					boolean isHtml = domainDataType.equals(DataTypes.html);

					GxWebView webView = new GxWebView(context, coordinator, item);
					webView.setMode(isHtml);

					if (layoutMode == LayoutModes.LIST)
					{
						// Set up the WebView so that the ListView's performItemClick() is fired.
						// See http://stackoverflow.com/questions/4973228/android-webview-inside-listview-onclick-event-issues
						webView.setEnabled(false);
						webView.setClickable(false);
				        webView.setLongClickable(false);
				        webView.setFocusable(false);
				        webView.setFocusableInTouchMode(false);
					}

					if (isHtml)
					{
						webView.setThemeClass(item.getThemeClass());
						webView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
					}

					// needed to get html to auto size
					int size = Services.Device.dipsToPixels(64);
					layout.setMinimumHeight(size);
					layout.addEdit(webView);
					attView = webView;
				}
				else //is an url
				{
					GxTextView textView = new GxTextView(context, item);
					layout.addEdit(textView);
					attView = textView;
				}
			}
			else if (item.getControlType().equals(ControlTypes.VideoView))
			{
				GxVideoView textView = new GxVideoView(context, item);
				layout.addEdit(textView);
				attView = textView;
			}
			else if (item.getControlType().equals(ControlTypes.AudioView))
			{
				GxAudioView textView = new GxAudioView(context, item);
				textView.setGravity(item.CellGravity);
				layout.addEdit(textView);
				attView = textView;
			}
			else
			{
				GxTextView textView = new GxTextView(context, coordinator, item);
				textView.setGravity(item.CellGravity);
				layout.addEdit(textView);

				attView = textView;
			}
		}

		return attView;
	}


	private static IGxEdit createAttEditControl(Context context, Coordinator coordinator, ImageLoader loader, LayoutItemDefinition item, DataBoundControl layout)
	{
		ArrayList<IGxEdit> editables = new ArrayList<IGxEdit>();
		if (item != null)
		{
			View result = TrnHelper.addEditField(context, coordinator, loader, item, editables);
			if (result!=null && result instanceof IGxEdit)
			{
				IGxEdit edit = (IGxEdit) result;
				layout.addEdit((View) edit.getEditControl());
				return edit;
			}

			//Default textview
			GxTextView textView = new GxTextView(context, item);
			textView.setGravity(item.CellGravity);
			layout.addEdit(textView);
			return textView;
		}
		return null;
	}

	private static void addActionIconToLabelAndFieldLayout(Context context, LayoutItemDefinition item, DataBoundControl layout, IGxEdit attView)
	{
		if (item != null)
		{
			DataTypeName domainDefinition = item.getDataTypeName();
		    RelationDefinition relDef = item.getFK();
		    boolean hasAction = false;

			if (relDef != null && item.getAutoLink())
			{
				ImageView image = new ImageView(context);
				image.setPadding(0, 0, Services.Device.dipsToPixels(8), 0);
				StandardImages.setLinkImage(image);

				layout.addDomainActionImage(image);
				hasAction = true;
			}
			else if (domainDefinition != null && domainDefinition.GetActions().size() > 0)
			{
				if (!(attView instanceof IHandleSemanticDomain))
				{
					for (String domainAction : domainDefinition.GetActions())
					{
						// Because of the way it's shown and handled, we can only add one domain action
						// Pick the first one that the device supports.
						if (PhoneHelper.isDomainActionSupported(domainAction))
						{
							ImageView image = new ImageView(context);
							StandardImages.setActionImage(image, domainAction);

							layout.addDomainActionImage(image);
							hasAction = true;
							break;
						}
					}
				}
			}

			if (hasAction)
			{
				//set gravity to att field
				LayoutParams parameters = (LayoutParams)((View)attView).getLayoutParams();
				parameters.gravity = item.CellGravity;
				parameters.weight = 1;
				((View)attView).setLayoutParams(parameters);
			}
		}
	}

	private static void addPromptIcon(DataBoundControl layout, IGxEdit attView)
	{
		ImageView image = new ImageView(layout.getContext());
		StandardImages.setPromptImage(image);
		layout.addFKActionImage(image);

		//set gravity to att field
		LayoutParams parameters = (LayoutParams)((View)attView).getLayoutParams();
		parameters.gravity = layout.getFormItemDefinition().CellGravity;
		parameters.weight = 1;
		((View)attView).setLayoutParams(parameters);
	}

	public static boolean isAdsTable(LayoutItemDefinition item)
	{
		//Temp , table for ads
		return item.getType().equalsIgnoreCase(LayoutItemsTypes.Table)
				&& (item.getName().equalsIgnoreCase("GoogleAdsControl"));
	}


}
