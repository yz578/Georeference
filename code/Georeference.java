package edu.cornell.georeference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

/**
 * This class allows the user to query through their indices to find desired places.
 * 
 * @author Yang Yang Zheng
 *
 */
public class Georeference {
	
	// Index Directories
	Directory modernIndex;
	Directory histIndex;
	
	// Search options
	public static final int MODERN_INDEX_ONLY = 1;
	public static final int HIST_INDEX_ONLY = 2;
	public static final int BOTH = 3;
	
    private String locSplitString;
    private String latLngSplitString;
    private int hitsPerPage;
    private double defaultRange;
    
    private Analyzer nameAnalyzer;
    
    // The distance scoring capability is bugged; it worked when used on a smaller index and breaks
    // on big index.
    private boolean distanceScore; 
    
    /**
     * Create a georeference object by specifying the indices and a bunch of details
     * 
     * @param modernIndex  the index/directory that stores the modern day places
     * @param histIndex  the index/directory that stores the historical places
     * @param locSplitString  the string that is used to separate points in bound and near points
     * @param latLngSplitString  the string that is used to separate different attributes of a point
     * in bound and near points
     * @param hitsPerPage  the max number of results from a search
     * @param defaultRange  the default range/distance (in miles) from the specified point before the result
     * got excluded (if there are multiple near points, the result is only excluded if it is not in the ranges
     * for all of the near points).
     */
    public Georeference(Directory modernIndex, Directory histIndex, String locSplitString, 
    		String latLngSplitString, int hitsPerPage, double defaultRange) {
    	this.modernIndex = modernIndex;
    	this.histIndex = histIndex;
    	this.locSplitString = locSplitString;
    	this.latLngSplitString = latLngSplitString;
    	this.hitsPerPage = hitsPerPage;
    	this.defaultRange = defaultRange;
    	
    	// Choose a default analyzer for name queries
    	nameAnalyzer = new KeywordAnalyzer();
    	
    	// Set distanceScore to false, because it is not fully working
    	distanceScore = false;
    }
    
	/**
     * Create a georeference object by specifying the indices and use default values for other fields.
     * 
     * @param modernIndex  the index/directory that stores the modern day places
     * @param histIndex  the index/directory that stores the historical places
     */
    public Georeference(Directory modernIndex, Directory histIndex) {
    	this.modernIndex = modernIndex;
    	this.histIndex = histIndex;
    	
    	// Set these to some default values;
    	locSplitString = ";";
    	latLngSplitString = ",";
    	hitsPerPage = 20;
    	defaultRange = Double.MAX_VALUE;
    	
    	// Choose a default analyzer for name queries
    	nameAnalyzer = new KeywordAnalyzer();
    
    	// Set distanceScore to false, because it is not fully working
    	distanceScore = false;
    }
    
    // Getters and setters
    
    public Directory getModernIndex() {
		return modernIndex;
	}

	public void setModernIndex(Directory modernIndex) {
		this.modernIndex = modernIndex;
	}

	public Directory getHistIndex() {
		return histIndex;
	}

	public void setHistIndex(Directory histIndex) {
		this.histIndex = histIndex;
	}

	public String getLocSplitString() {
		return locSplitString;
	}

	public void setLocSplitString(String locSplitString) {
		this.locSplitString = locSplitString;
	}

	public String getLatLngSplitString() {
		return latLngSplitString;
	}

	public void setLatLngSplitString(String latLngSplitString) {
		this.latLngSplitString = latLngSplitString;
	}

	public int getHitsPerPage() {
		return hitsPerPage;
	}

	public void setHitsPerPage(int hitsPerPage) {
		this.hitsPerPage = hitsPerPage;
	}

	public double getDefaultRange() {
		return defaultRange;
	}

	public void setDefaultRange(double defaultRange) {
		this.defaultRange = defaultRange;
	}
    
    /**
     * This method can search through the indices to find the latitude-longitude coordinate of the place 
     * you are looking for by returning a list of possible matching results. 
     * 
     * @param placeName  the name of the place you want to search for
     * @param bound  a boundary box that filters out places outside of the box (default format: 0,0;1,1)
     * @param nearPoints  points and their ranges (in miles) so that they can affect the score (default 
     * format(0,0,100;1,1,1000;...); if the ranges are not specified, the method will use the default
     * range value
     * @param searchOption  controls with index to search on
     * @param distanceScoreOption  how distances from the near points affect the score
     * @return a string in xml format that lists out all places (name and their latitude and longitude) 
     * that match the search; each search result is scored; the search result list is sorted by their
     * score
     * @throws CorruptIndexException
     * @throws IOException
     * @throws ParseException 
     */
    public String searchLocation(String placeName, String bound, String nearPoints, int searchOption, 
    		int distanceScoreOption) throws CorruptIndexException, IOException, ParseException {
		
    	IndexSearcher searcher = createSearcher(searchOption);
	    
	    // Build the query
		Query query = createNameBoundQuery(placeName, bound);
		List<DistanceQueryBuilder> distanceQueries = createDistanceQueries(nearPoints);
		query = includeDistanceQueries(query, distanceQueries, distanceScoreOption);
		
		// Perform the search
		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
		return getXMLFromHits(searcher, hits);
	}
    
    // Other versions of the same method above
    
	/**
     * This method can search through the indices to find the latitude-longitude coordinate of the place 
     * you are looking for by returning a list of possible matching results. It uses the default
     * searchOption BOTH and the default distanceScoreOption SQUARE_ROOT.
     * 
     * @param placeName  the name of the place you want to search for
     * @param bound  a boundary box that filters out places outside of the box (default format: 0,0;1,1)
     * @param nearPoints  points and their ranges (in miles) so that they can affect the score (default 
     * format(0,0,100;1,1,1000;...); if the ranges are not specified, the method will use the default
     * range value
     * @return a string in xml format that lists out all places (name and their latitude and longitude) 
     * that match the search; each search result is scored; the search result list is sorted by their
     * score
     * @throws IOException
	 * @throws ParseException 
     */
    public String searchLocation(String placeName, String bound, String nearPoints) throws IOException, ParseException {
    	return searchLocation(placeName, bound, nearPoints, BOTH, DistanceScoreQuery.SQUARE_ROOT);
    }
    
    /**
     * This method can search through the indices to find the latitude-longitude coordinate of the place 
     * you are looking for by returning a list of possible matching results. It uses the default 
     * distanceScoreOption SQUARE_ROOT.
     * 
     * @param placeName  the name of the place you want to search for
     * @param bound  a boundary box that filters out places outside of the box (default format: 0,0;1,1)
     * @param nearPoints  points and their ranges (in miles) so that they can affect the score (default 
     * format(0,0,100;1,1,1000;...); if the ranges are not specified, the method will use the default
     * range value
     * @param searchOption  controls with index to search on
     * @return a string in xml format that lists out all places (name and their latitude and longitude) 
     * that match the search; each search result is scored; the search result list is sorted by their
     * score
     * @throws CorruptIndexException
     * @throws IOException
     * @throws ParseException 
     */
    public String searchLocation(String placeName, String bound, String nearPoints, int searchOption) throws IOException, ParseException {
    	return searchLocation(placeName, bound, nearPoints, searchOption, DistanceScoreQuery.SQUARE_ROOT);
    }
    
    /**
     * This method finds a list of nearby places to the specified place. The score depends on the distance
     * of the result from the specified place.
     * 
     * @param placeName  the name of the place you want to search for
     * @param latitude  the latitude of the place you want to search for
     * @param longitude the longitude of the place you want to search for
     * @param rangeInMiles  only return results in the specified range (in miles)
     * @param inclusive  whether or not to include the specified place in the result list
     * @param searchOption  controls with index to search on
     * @param distanceScoreOption  how the distance from the specified place to the result affects the score
     * @return a string in xml format that lists out all places (name and their latitude and longitude) 
     * that are in the range; each search result is scored; the search result list is sorted by their
     * score
     * @throws IOException
     * @throws ParseException 
     */
    public String searchNearbyPlaces(String placeName, double latitude, double longitude, double rangeInMiles, boolean inclusive, int searchOption ,int distanceScoreOption) throws IOException, ParseException {
    	IndexSearcher searcher = createSearcher(searchOption);
    	
    	// Build the query
    	DistanceQueryBuilder dq = new DistanceQueryBuilder(latitude, longitude, rangeInMiles, 
				GeoIndexWriter.SPT_LAT_FIELD, GeoIndexWriter.SPT_LNG_FIELD, GeoIndexWriter.TIER_PREFIX_FIELD, 
				true, GeoIndexWriter.START_TIER, GeoIndexWriter.END_TIER);
    	BooleanQuery bq = new BooleanQuery();
    	bq.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
    	if (!inclusive) {
	    	Query nameQuery = createNameQuery(placeName);
	    	if (nameQuery != null) {
	    		bq.add(nameQuery, BooleanClause.Occur.MUST_NOT);
	    	}
    	}
    	
    	// Whether or not to use DistanceScoreQuery to allow it score the results by distance
    	Query query = null;
    	if (distanceScore) {
    		query = new DistanceScoreQuery(dq.getQuery(bq), dq, distanceScoreOption);
    	}
    	else {
    		query = dq.getQuery(bq);
    	}
    	
    	// Perform the search
    	TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
    	searcher.search(query, collector);
    	ScoreDoc[] hits = collector.topDocs().scoreDocs;
    	
    	return getXMLFromHits(searcher, hits);
    }
    
    // Other versions of the same method above
    
    /**
    * This method finds a list of nearby places to the specified place. The score depends on the distance
    * of the result from the specified place.
    * 
    * @param placeName  the name of the place you want to search for
    * @param pointInfo  the latitude and longitude of the specified place and the range/distance (in miles) you 
    * want your results to be in (default format: 0,0,100)
    * @param inclusive  whether or not to include the specified place in the result list
    * @param searchOption  controls with index to search on
    * @param distanceScoreOption  how the distance from the specified place to the result affects the score
    * @return a string in xml format that lists out all places (name and their latitude and longitude) 
     * that are in the range; each search result is scored; the search result list is sorted by their
     * score
    * @throws IOException
     * @throws ParseException 
    */
    public String searchNearbyPlaces(String placeName, String pointInfo, boolean inclusive, int searchOption ,int distanceScoreOption) throws IOException, ParseException {
    	double[][] point = getNearbyPoints(pointInfo);
    	if (point == null) {
    		return null;
    	}
    	return searchNearbyPlaces(placeName, point[0][0], point[0][1], point[0][2], inclusive, searchOption, distanceScoreOption);
    }
    
    /**
     * This method finds a list of nearby places to the specified place. The score depends on the distance
     * of the result from the specified place. It uses the default searchOption BOTH and the default 
     * distanceScoreOption SQUARE_ROOT. It is also set to not include the specified place in the results.
     * 
     * @param placeName  the name of the place you want to search for
     * @param pointInfo  the latitude and longitude of the specified place and the range/distance (in miles) you 
     * want your results to be in (default format: 0,0,100)
     * @return a string in xml format that lists out all places (name and their latitude and longitude) 
     * that are in the range; each search result is scored; the search result list is sorted by their
     * score
     * @throws IOException
     * @throws ParseException 
     */
    
    public String searchNearbyPlaces(String placeName, String pointInfo) throws IOException, ParseException {
    	return searchNearbyPlaces(placeName, pointInfo, false, BOTH, DistanceScoreQuery.SQUARE_ROOT);
    }
    
    /**
     * This method finds a list of nearby places to the specified place. The score depends on the distance
     * of the result from the specified place. It uses the default distanceScoreOption SQUARE_ROOT. 
     * It is also set to not include the specified place in the results.
     * 
     * @param placeName  the name of the place you want to search for
     * @param pointInfo  the latitude and longitude of the specified place and the range/distance (in miles) you 
     * want your results to be in (default format: 0,0,100)
     * @param searchOption  controls with index to search on
     * @return a string in xml format that lists out all places (name and their latitude and longitude) 
     * that are in the range; each search result is scored; the search result list is sorted by their
     * score
     * @throws IOException
     * @throws ParseException 
     */
    public String searchNearbyPlaces(String placeName, String pointInfo, int searchOption) throws IOException, ParseException {
    	return searchNearbyPlaces(placeName, pointInfo, false, searchOption, DistanceScoreQuery.SQUARE_ROOT);
    }
    
    /**
     * This method finds a list of nearby places to the specified place. The score depends on the distance
     * of the result from the specified place. It uses the default searchOption BOTH and the default 
     * distanceScoreOption SQUARE_ROOT. It is also set to not include the specified place in the results.
     * 
     * @param placeName  the name of the place you want to search for
     * @param latitude  the latitude of the place you want to search for
     * @param longitude the longitude of the place you want to search for
     * @param rangeInMiles  only return results in the specified range (in miles)
     * @return a string in xml format that lists out all places (name and their latitude and longitude) 
     * that are in the range; each search result is scored; the search result list is sorted by their
     * score
     * @throws IOException
     * @throws ParseException 
     */
    public String searchNearbyPlaces(String placeName, double latitude, double longitude, double rangeInMiles) throws IOException, ParseException {
    	return searchNearbyPlaces(placeName, latitude, longitude, rangeInMiles, false, BOTH, DistanceScoreQuery.SQUARE_ROOT);
    }
    
    // All of methods below are private helper methods used by the public methods above.
    
    // Create the index searcher differently depending on the search option
    private IndexSearcher createSearcher(int searchOption) throws CorruptIndexException, IOException {    	
    	IndexSearcher searcher = null;
	    if (searchOption == MODERN_INDEX_ONLY) {
	    	searcher = new IndexSearcher(modernIndex, true);
	    }
	    else if (searchOption == HIST_INDEX_ONLY) {
	    	searcher = new IndexSearcher(histIndex, true);
	    }
	    else if (searchOption == BOTH) {
	    	// Create a searcher for each index and then combine them
	    	IndexReader[] readers = new IndexReader[2];
	    	readers[0] = IndexReader.open(modernIndex, true);
	    	readers[1] = IndexReader.open(histIndex, true);
	    	searcher = new IndexSearcher(new MultiReader(readers));
	    }
	    return searcher;
    }
	
    // Create the boundary from two points
	private double[][] mapBoundaryPoints(String[] point1, String[] point2) {
		double[][] boundary = new double[2][2];
		double point1a = Double.parseDouble(point1[0]);
		double point2a = Double.parseDouble(point2[0]);
		double point1b = Double.parseDouble(point1[1]);
		double point2b = Double.parseDouble(point2[1]);
		
		// Decided where to place the attribute
		if (point1a > point2a) {
			boundary[0][0] = point2a;
			boundary[0][1] = point1a;
		}
		else {
			boundary[0][0] = point1a;
			boundary[0][1] = point2a;
		}
		if (point1b > point2b) {
			boundary[1][0] = point2b;
			boundary[1][1] = point1b;
		}
		else {
			boundary[1][0] = point1b;
			boundary[1][1] = point2b;
		}
		return boundary;
	}
	
	// Create nearby points array from a string
	private double[][] getNearbyPoints(String nearbyPoints) {
		String[] locationTerm = nearbyPoints.split(locSplitString);
		double[][] out = new double[locationTerm.length][3];
		
		// Create the point from the split string
		String[] point;
		double miles;
		for (int i = 0; i < locationTerm.length; i++) {
			point = locationTerm[i].split(latLngSplitString);
			
			// Use default range if necessary
			if (point.length == 3) {
				if (point[2].equals("")) {
					miles = defaultRange;
				}
				else {
					miles = Double.parseDouble(point[2]);
				}
			}
			else if (point.length == 2){
				miles = defaultRange;
			}
			else {
				return null;
			}
			
			out[i][0] = Double.parseDouble(point[0]);
			out[i][1] = Double.parseDouble(point[1]);
			out[i][2] = miles;
		}
		return out;
	}
	
	// Create the part of the query involving name and boundary
	private Query createNameBoundQuery(String nameTerm, String locationTerm) throws ParseException {
		BooleanQuery booleanQuery = new BooleanQuery();
		
		// Add the name query to the boolean query
		Query nameQuery = createNameQuery(nameTerm);
		if (nameQuery != null) {
			booleanQuery.add(nameQuery, BooleanClause.Occur.MUST);
		}
		
		// Add the bound queries to the boolean query
		Query[] boundQueries = createBoundQueries(locationTerm);
		if (boundQueries != null) {
			for (int i = 0; i < boundQueries.length; i++) {
				booleanQuery.add(boundQueries[i], BooleanClause.Occur.MUST);
			}
		}
		
		return booleanQuery;
	}
	
	// Create the name query
	private Query createNameQuery(String nameTerm) throws ParseException {
		if (nameTerm != null) {
			if (!nameTerm.equals("")) {
				String nameTermLower = nameTerm.toLowerCase(); 
				
				// Create a term query for each name in the document
				QueryParser parser = new QueryParser(Version.LUCENE_34, GeoIndexWriter.NAME_FIELD, nameAnalyzer);
				String queryString = GeoIndexWriter.NAME_FIELD + ":" + nameTermLower;
				Query nameQuery = parser.parse(queryString);
				queryString = GeoIndexWriter.ALT_NAME1_FIELD + ":" + nameTermLower;
				Query altName1Query = parser.parse(queryString);
				queryString = GeoIndexWriter.ALT_NAME2_FIELD + ":" + nameTermLower;
				Query altName2Query = parser.parse(queryString);
				
				// Add the term queries to a boolean query
				BooleanQuery nameQueries = new BooleanQuery();
				nameQueries.add(nameQuery, BooleanClause.Occur.SHOULD);
				nameQueries.add(altName1Query, BooleanClause.Occur.SHOULD);
				nameQueries.add(altName2Query, BooleanClause.Occur.SHOULD);
				
				return nameQueries;
			}
		}
		return null;
	}

	// Create the bound query
	private Query[] createBoundQueries(String locationTerm) {
		if (locationTerm != null) {
			if (!locationTerm.equals("")) {
				String[] locationTerm1 = locationTerm.split(locSplitString);
				if (locationTerm1.length == 2) {
					String[] point1 = locationTerm1[0].split(latLngSplitString);
					String[] point2 = locationTerm1[1].split(latLngSplitString);
					double[][] boundary = mapBoundaryPoints(point1, point2);
					
					// Create an range query for latitude and one for longitude
					NumericRangeQuery<Double> latitudeQuery = NumericRangeQuery.newDoubleRange(GeoIndexWriter.LATITUDE_FIELD, boundary[0][0], boundary[0][1], true, true);;
					NumericRangeQuery<Double> longitudeQuery = NumericRangeQuery.newDoubleRange(GeoIndexWriter.LONGITUDE_FIELD, boundary[1][0], boundary[1][1], true, true);;
					
					// Add the range queries to the array
					Query[] queries = new NumericRangeQuery[2];
					queries[0] = latitudeQuery;
					queries[1] = longitudeQuery;
					
					return queries;
				}
			}
		}
		return null;
	}
	
	// Create the distance part of the query
	private List<DistanceQueryBuilder> createDistanceQueries(String locationTerm) {
		ArrayList<DistanceQueryBuilder> list = new ArrayList<DistanceQueryBuilder>();
		if (locationTerm != null) {
			if (!locationTerm.equals("")) {
				double[][] locationTerm1 = getNearbyPoints(locationTerm);
				if (locationTerm1 == null) {
					return list;
				}
				
				// Make a distance query for each near point
				DistanceQueryBuilder dq;
				for (int i = 0; i < locationTerm1.length; i++) {
					dq = new DistanceQueryBuilder(locationTerm1[i][0], locationTerm1[i][1], 
							locationTerm1[i][2], GeoIndexWriter.SPT_LAT_FIELD, 
							GeoIndexWriter.SPT_LNG_FIELD, GeoIndexWriter.TIER_PREFIX_FIELD, 
							true, GeoIndexWriter.START_TIER, GeoIndexWriter.END_TIER);
					list.add(dq);
				}
			}
		}
		return list;
	}
	
	// Add/combine the distance query to the current query
	private Query includeDistanceQueries(Query query, List<DistanceQueryBuilder> dqs, int distanceScoreOption) {
		if (dqs.size() != 0) {
			BooleanQuery bq = new BooleanQuery();
			
			// Create a DistanceScoreQuery for each distance query
			for (final DistanceQueryBuilder dq : dqs) {
				
				// Whether or not to use DistanceScoreQuery to allow it score the results by distance
		    	Query q = null;
		    	if (distanceScore) {
		    		q = new DistanceScoreQuery(dq.getQuery(query), dq, distanceScoreOption);
		    	}
		    	else {
		    		q = dq.getQuery(query);
		    	}
		    	bq.add(q, BooleanClause.Occur.SHOULD);
			}
			return bq;
		}
		else {
			return query;
		}
	}
	
	// Convert the search result to a XML format
	private String getXMLFromHits(IndexSearcher searcher, ScoreDoc[] hits) throws CorruptIndexException, IOException {
		String out = "<?xml version=\"1.0\"?>\n<places>\n";
		for(int i = 0; i < hits.length; i++) {
			out += getXMLForGeoScoreDoc(searcher, hits[i]);
		}
		out += "</places>";
		return out;
	}
	
	// Convert a geolocation document to a XML format
	private String getXMLForGeoScoreDoc(IndexSearcher searcher, ScoreDoc scoreDoc) throws CorruptIndexException, IOException {
		int docId = scoreDoc.doc;
		Document document = searcher.doc(docId);
		String out = "";
		out += "<place>\n";
	    out += "<place_name>" + document.get(GeoIndexWriter.NAME_FIELD) + "</place_name>\n";
	    out += "<doc_id>" + docId + "</doc_id>\n";
	    out += "<latitude>" + document.get(GeoIndexWriter.LATITUDE_FIELD) + "</latitude>\n";
	    out += "<longitude>" + document.get(GeoIndexWriter.LONGITUDE_FIELD) + "</longitude>\n";
	    out += "<score>" + scoreDoc.score + "</score>\n";
		out += "</place>\n";
		return out;
	}
	
}
