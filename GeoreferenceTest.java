package edu.cornell.georeference;

import java.io.IOException;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.Directory;

public class GeoreferenceTest {
	public static void main(String[] args) throws IOException, ParseException {
		GeoIndexWriter geoIndexWriter = new GeoIndexWriter();
		Directory index = geoIndexWriter.getGeonameDirectory("data/cities1000.txt", "index/geoname");
		Directory index2 = geoIndexWriter.getHistnameDirectory("data/pleiades-names.txt", "index/histname");
		
		Georeference geo = new Georeference(false);
		System.out.println(geo.searchLocation(index, index2, "washington", null, "54.9, -1.5, 1000/53.9,-1.5, 10"));
	}
}
