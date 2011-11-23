package edu.cornell.georeference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.store.Directory;

public class Georeference {
	
	// Defaults
    private String locSplitString = "/";
    private String latLngSplitString = ",";
    private boolean scoreByDistance;
    private int hitsPerPage = 20;
    
    public Georeference(boolean scoreByDistance) {
    	this.scoreByDistance = scoreByDistance;
    }
    
    public void setLocationSplitString(String newSplitString) {
    	locSplitString = newSplitString;
    }
    
    public void setLatLngSplitString(String newSplitString) {
    	latLngSplitString = newSplitString;
    }
    
    public void setHitsPerPage(int newHitsPerPage) {
    	hitsPerPage = newHitsPerPage;
    }
    
    public String searchLocation(Directory modernIndex, Directory histIndex, String placeName, String bound, String nearPoints) throws IOException  {
		
	    IndexSearcher searcher = new IndexSearcher(modernIndex, true);
	    IndexSearcher searcher2 = new IndexSearcher(histIndex, true);
	    
		Query query = createGeonameQuery(placeName, bound);
		
		List<DistanceQueryBuilder> distanceQueries = createDistanceQueries(nearPoints);
		query = includeDistanceQueries(query, distanceQueries);
		
		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		List<Map<Integer, Double>> distances = getDistances(distanceQueries);
	    
	    String out = "<places xmlns=\"http://www.w3schools.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema\" xsi:schemaLocation=\"georeference.xsd\">\n";
		if (hits.length > 0) {
			for(int i = 0; i < hits.length; i++) {
			    out += getXMLForGeonameScoreDoc(searcher, hits[i], distances);
			}
		}
		else {
			query = createHistnameQuery(placeName, bound);
			query = includeDistanceQueries(query, distanceQueries);
			searcher2.search(query, collector);
			ScoreDoc[] hits2 = collector.topDocs().scoreDocs;
			distances = getDistances(distanceQueries);
			if (hits2.length > 0) {
				for(int i = 0; i < hits2.length; i++) {
					out += getXMLForHistScoreDoc(searcher2, hits2[i], distances);
				}
			}
		}
		out += "</places>";
		
		return out;
	}
	
	private double[][] mapBoundaryPoints(String[] point1, String[] point2) {
		double[][] boundary = new double[2][2];
		double point1a = Double.parseDouble(point1[0]);
		double point2a = Double.parseDouble(point2[0]);
		double point1b = Double.parseDouble(point1[1]);
		double point2b = Double.parseDouble(point2[1]);
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
	
	private Query createGeonameQuery(String nameTerm, String locationTerm) {
		BooleanQuery booleanQuery = new BooleanQuery();
		if (nameTerm != null) {
			String nameTermLower = nameTerm.toLowerCase();
			TermQuery nameQuery = new TermQuery(new Term("name", nameTermLower));
			TermQuery asciinameQuery = new TermQuery(new Term("asciiname", nameTermLower));
			TermQuery alternatenamesQuery = new TermQuery(new Term("alternatenames", nameTermLower));
			BooleanQuery nameQueries = new BooleanQuery();
			nameQueries.add(nameQuery, BooleanClause.Occur.SHOULD);
			nameQueries.add(asciinameQuery, BooleanClause.Occur.SHOULD);
			nameQueries.add(alternatenamesQuery, BooleanClause.Occur.SHOULD);
			booleanQuery.add(nameQueries, BooleanClause.Occur.MUST);
		}
		
		if (locationTerm != null) {
			String[] locationTerm1 = locationTerm.split(locSplitString);
			if (locationTerm1.length == 2) {
				String[] point1 = locationTerm1[0].split(latLngSplitString);
				String[] point2 = locationTerm1[1].split(latLngSplitString);
				double[][] boundary = mapBoundaryPoints(point1, point2);
				NumericRangeQuery<Double> latitudeQuery = NumericRangeQuery.newDoubleRange("latitude", boundary[0][0], boundary[0][1], true, true);;
				booleanQuery.add(latitudeQuery, BooleanClause.Occur.MUST);
				NumericRangeQuery<Double> longitudeQuery = NumericRangeQuery.newDoubleRange("longitude", boundary[1][0], boundary[1][1], true, true);;
				booleanQuery.add(longitudeQuery, BooleanClause.Occur.MUST);
			}
		}
		
		return booleanQuery;
	}
	
	private Query createHistnameQuery(String nameTerm, String locationTerm) {
		BooleanQuery booleanQuery = new BooleanQuery();
		if (nameTerm != null) {
			TermQuery titleQuery = new TermQuery(new Term("title", nameTerm));
			TermQuery nameAttestedQuery = new TermQuery(new Term("nameAttested", nameTerm));
			TermQuery nameTransliteratedQuery = new TermQuery(new Term("nameTransliterated", nameTerm));
			BooleanQuery nameQueries = new BooleanQuery();
			nameQueries.add(titleQuery, BooleanClause.Occur.SHOULD);
			nameQueries.add(nameAttestedQuery, BooleanClause.Occur.SHOULD);
			nameQueries.add(nameTransliteratedQuery, BooleanClause.Occur.SHOULD);
			booleanQuery.add(nameQueries, BooleanClause.Occur.MUST);
		}
		
		if (locationTerm != null) {
			String[] locationTerm1 = locationTerm.split(locSplitString);
			if (locationTerm1.length == 2) {
				String[] point1 = locationTerm1[0].split(latLngSplitString);
				String[] point2 = locationTerm1[1].split(latLngSplitString);
				double[][] boundary = mapBoundaryPoints(point1, point2);
				NumericRangeQuery<Double> latitudeQuery = NumericRangeQuery.newDoubleRange("reprLat", boundary[0][0], boundary[0][1], true, true);;
				booleanQuery.add(latitudeQuery, BooleanClause.Occur.MUST);
				NumericRangeQuery<Double> longitudeQuery = NumericRangeQuery.newDoubleRange("reprLong", boundary[1][0], boundary[1][1], true, true);;
				booleanQuery.add(longitudeQuery, BooleanClause.Occur.MUST);
			}
		}
		
		return booleanQuery;
	}
	
	private List<DistanceQueryBuilder> createDistanceQueries(String locationTerm) {
		ArrayList<DistanceQueryBuilder> list = new ArrayList<DistanceQueryBuilder>();
		if (locationTerm != null) {
			String[] locationTerm1 = locationTerm.split(locSplitString);
			String[] point;
			DistanceQueryBuilder dq;
			double miles;
			for (int i = 0; i < locationTerm1.length; i++) {
				point = locationTerm1[i].split(latLngSplitString);
				if (point.length == 3) {
					miles = Double.parseDouble(point[2]);
				}
				else {
					miles = Double.MAX_VALUE;
				}
				dq = new DistanceQueryBuilder(Double.parseDouble(point[0]), 
						Double.parseDouble(point[1]), 
						miles, 
						GeoIndexWriter.LAT_FIELD, 
						GeoIndexWriter.LNG_FIELD, GeoIndexWriter.TIER_PREFIX_FIELD, 
						true, GeoIndexWriter.START_TIER, GeoIndexWriter.END_TIER);
				list.add(dq);
			}
		}
		return list;
	}
	
	private Query includeDistanceQueries(Query query, List<DistanceQueryBuilder> dqs) {
		if (dqs.size() != 0) {
			BooleanQuery bq = new BooleanQuery();
			//bq.add(query, BooleanClause.Occur.SHOULD);
			for (DistanceQueryBuilder dq : dqs) {
				bq.add(dq.getQuery(query), BooleanClause.Occur.SHOULD);
			}
			return bq;
		}
		else {
			return query;
		}
	}
	
	private List<Map<Integer, Double>> getDistances(List<DistanceQueryBuilder> dqs) {
		ArrayList<Map<Integer, Double>> distances = new ArrayList<Map<Integer, Double>>();
		for (DistanceQueryBuilder dq : dqs) {
			distances.add(dq.getDistanceFilter().getDistances());
		}
		return distances;
	}
	
	private String getXMLForGeonameScoreDoc(IndexSearcher searcher, ScoreDoc scoreDoc, List<Map<Integer, Double>> distances) throws CorruptIndexException, IOException {
		int docId = scoreDoc.doc;
		Document document = searcher.doc(docId);
		String out = "";
		out += "<place>\n";
	    out += "<place_name>" + document.get("name") + "</place_name>\n";
	    out += "<latitude>" + document.get("latitude") + "</latitude>\n";
	    out += "<longitude>" + document.get("longitude") + "</longitude>\n";
	    if (scoreByDistance) {
	    	out += "<score>" + calculateScore(docId, distances) + "</score>\n";
	    }
	    else {
	    	out += "<score>" + scoreDoc.score + "</score>\n";
	    }
		out += "</place>\n";
		return out;
	}
	
	private String getXMLForHistScoreDoc(IndexSearcher searcher, ScoreDoc scoreDoc, List<Map<Integer, Double>> distances) throws CorruptIndexException, IOException {
		int docId = scoreDoc.doc;
		Document document = searcher.doc(docId);
		String out = "";
		out += "<place>\n";
	    out += "<place_name>" + document.get("title") + "</place_name>\n";
	    out += "<latitude>" + document.get("reprLat") + "</latitude>\n";
	    out += "<longitude>" + document.get("reprLong") + "</longitude>\n";
	    if (scoreByDistance) {
	    	out += "<score>" + calculateScore(docId, distances) + "</score>\n";
	    }
	    else {
	    	out += "<score>" + scoreDoc.score + "</score>\n";
	    }
	    out += "</place>\n";
		return out;
	}
	
	private double calculateScore(int docId, List<Map<Integer, Double>> distances) {
		double score = 0.99;
		double sum = 0;
		Double v;
		for (Map<Integer, Double> d : distances) {
			v = (Double) d.get(docId);
			if (v == null) {
				sum = Double.MAX_VALUE;
				break;
			}
			else {
				sum += v;
			}
			
		}
		sum = Math.sqrt(sum);
		if (sum == 0) {
			return score;
		}
		else {
			return Math.pow(score, sum);
		}
	}
	
}
