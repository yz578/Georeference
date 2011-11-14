package georeference;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.spatial.tier.DistanceFieldComparatorSource;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.queryParser.surround.query.DistanceQuery;

public class Georeference {

    private String locSplitString = "/";
    private String latLngSplitString = ",";
    
    public Georeference() {
    	
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
			//nameQuery.setBoost((float) 1.5);
			TermQuery asciinameQuery = new TermQuery(new Term("asciiname", nameTermLower));
			//asciinameQuery.setBoost((float) 1.3);
			TermQuery alternatenamesQuery = new TermQuery(new Term("alternatenames", nameTermLower));
			//asciinameQuery.setBoost((float) 1.0);
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
			for (int i = 0; i < locationTerm1.length; i++) {
				point = locationTerm1[i].split(latLngSplitString);
				dq = new DistanceQueryBuilder(Double.parseDouble(point[0]), 
						Double.parseDouble(point[1]), 
						Double.MAX_VALUE, GeoIndexWriter.LAT_FIELD, 
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
			for (DistanceQueryBuilder dq : dqs) {
				bq.add(dq.getQuery(query), BooleanClause.Occur.SHOULD);
			}
			return bq;
		}
		else {
			return query;
		}
	}
	
	private List<Map> getDistances(List<DistanceQueryBuilder> dqs) {
		ArrayList<Map> distances = new ArrayList<Map>();
		for (DistanceQueryBuilder dq : dqs) {
			distances.add(dq.getDistanceFilter().getDistances());
		}
		return distances;
	}
	
	private String getXMLForGeonameScoreDoc(IndexSearcher searcher, ScoreDoc scoreDoc, List<Map> distances) throws CorruptIndexException, IOException {
		int docId = scoreDoc.doc;
		Document document = searcher.doc(docId);
		String out = "";
		out += "<place>\n";
	    out += "<place_name>" + document.get("name") + "</place_name>\n";
	    out += "<latitude>" + document.get("latitude") + "</latitude>\n";
	    out += "<longitude>" + document.get("longitude") + "</longitude>\n";
		/*
	    for (Map d : distances) {
			out += d.get(docId) + "\n";
		}
		out += "<score>" + scoreDoc.score + "</score>\n";
		*/
		 out += "<score>" + calculateScore(docId, distances) + "</score>\n";
		out += "</place>\n";
		return out;
	}
	
	private String getXMLForHistScoreDoc(IndexSearcher searcher, ScoreDoc scoreDoc, List<Map> distances) throws CorruptIndexException, IOException {
		int docId = scoreDoc.doc;
		Document document = searcher.doc(docId);
		String out = "";
		out += "<place>\n";
	    out += "<place_name>" + document.get("title") + "</place_name>\n";
	    out += "<latitude>" + document.get("reprLat") + "</latitude>\n";
	    out += "<longitude>" + document.get("reprLong") + "</longitude>\n";
	    /*
	    for (Map d : distances) {
			out += d.get(docId) + "\n";
		}
	    out += "<score>" + scoreDoc.score + "</score>\n";
	    */
	    out += "<score>" + calculateScore(docId, distances) + "</score>\n";
	    out += "</place>\n";
		return out;
	}
	
	private double calculateScore(int docId, List<Map> distances) {
		double score = 0.99;
		double sum = 0;
		Double v;
		for (Map d : distances) {
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
	
	public String searchLocation(Directory modernIndex, Directory histIndex, String placeName, String bound, String nearPoints) throws IOException  {
		
		int hitsPerPage = 20;
	    IndexSearcher searcher = new IndexSearcher(modernIndex, true);
	    IndexSearcher searcher2 = new IndexSearcher(histIndex, true);
	    
		Query query = createGeonameQuery(placeName, bound);
		
		List<DistanceQueryBuilder> distanceQueries = createDistanceQueries(nearPoints);
		query = includeDistanceQueries(query, distanceQueries);
		
		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		List<Map> distances = getDistances(distanceQueries);
	    
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
			System.out.println(hits2.length);
			distances = getDistances(distanceQueries);
			System.out.println(query);
			if (hits2.length > 0) {
				for(int i = 0; i < hits2.length; i++) {
					out += getXMLForHistScoreDoc(searcher2, hits2[i], distances);
				}
			}
		}
		out += "</places>";
		
		return out;
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		GeoIndexWriter geoIndexWriter = new GeoIndexWriter();
		Directory index = geoIndexWriter.getGeonameDirectory("data/cities1000.txt", "index/geoname");
		Directory index2 = geoIndexWriter.getHistnameDirectory("data/pleiades-names.txt", "index/histname");
		
		Georeference geo = new Georeference();
		System.out.println(geo.searchLocation(index, index2, "washington", "adssf", "30,30/21,34/55,31"));
	}
}
