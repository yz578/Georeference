package edu.cornell.georeference;

import java.io.IOException;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.Directory;
/**
 * Tests the Georeference
 * 
 * @author Yang Yang Zheng
 *
 */
public class GeoreferenceTest {
	public static void main(String[] args) throws IOException, ParseException {
		
		// Build the indices
		GeoDataSource modernDataSource = GeoDataSource.createGeoNamesDataSource("data/allCountries.txt");
		GeoDataSource histDataSource = GeoDataSource.createPleiadesDataSource("data/pleiades-names.txt");
		GeoIndexWriter geoIndexWriter = new GeoIndexWriter();
		Directory modernIndex = geoIndexWriter.buildIndexIfIndexNotExist(modernDataSource, "index/modname");
		Directory histIndex = geoIndexWriter.buildIndexIfIndexNotExist(histDataSource, "index/histname");
		System.out.println("Finish building indices");
		System.out.println("\n");
		
		// Perform some sample searched
		Georeference geo = new Georeference(modernIndex, histIndex);
		geo.setHitsPerPage(5);
		String out = geo.searchLocation("new york", null, null);
		System.out.println(out);
		System.out.println("\n");
		out = geo.searchLocation("richmond", null, "42,-76,100;40,-80,100;40.8,-75.14,10");
		System.out.println(out);
		System.out.println("\n");
		out = geo.searchLocation("ebura", "30,-4;40,0", null);
		System.out.println(out);
		System.out.println("\n");
		out = geo.searchNearbyPlaces("washington", "54.9,-1.5,10");
		System.out.println(out);
	}
}
