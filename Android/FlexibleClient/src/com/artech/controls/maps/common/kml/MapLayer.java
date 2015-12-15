package com.artech.controls.maps.common.kml;

import java.util.ArrayList;
import java.util.List;

import com.artech.controls.maps.common.IMapLocation;

/**
 * Definition of a Map Layer (loaded from a KML file).
 * Contains a number of MapFeatures, which can currently be polygons or polylines (LineString).
 * @author matiash
 */
public class MapLayer
{
	public String id;
	public final List<MapFeature> features;
	
	public MapLayer()
	{
		features = new ArrayList<MapLayer.MapFeature>();
	}
	
	public enum FeatureType { Polyline, Polygon }
	
	public static abstract class MapFeature
	{
		public final FeatureType type;
		
		public String id;
		public String name;
		public String description;
		
		// Place to store the actual "map implementation object"
		// (e.g. com.google.android.gms.maps.model.Polygon for polygons in Google Maps API V2).
		public Object mapObject;
		
		protected MapFeature(FeatureType type)
		{
			this.type = type;
		}
		
		public abstract List<IMapLocation> getPoints();
	}
	
	public static class Polygon extends MapFeature
	{
		public final List<IMapLocation> outerBoundary;
		public final List<List<IMapLocation>> holes;

		public Integer strokeColor;
		public Float strokeWidth;
		public Integer fillColor;
		
		public Polygon()
		{
			super(FeatureType.Polygon);
			outerBoundary = new ArrayList<IMapLocation>();
			holes = new ArrayList<List<IMapLocation>>();
		}

		@Override
		public List<IMapLocation> getPoints()
		{
			return outerBoundary;
		}
	}

	public static class Polyline extends MapFeature
	{
		public final List<IMapLocation> points;
		public Integer strokeColor;
		public Float strokeWidth;
		
		public Polyline()
		{
			super(FeatureType.Polyline);
			points = new ArrayList<IMapLocation>();
		}

		@Override
		public List<IMapLocation> getPoints()
		{
			return points;
		}
	}
}
