package edu.cornell.georeference;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.Directory;

/**
 * A servlet that uses the Georeference. It allows users to do the search through HTTP get.
 * 
 * @author Yang Yang
 *
 */
@SuppressWarnings("serial")
@WebServlet("/georeference")
public class GeoreferenceServlet extends HttpServlet {
	
	private Georeference geo;
	
	public void init() {
		
		System.out.println("SERVLET STATUS: Ready to initialize the service");
		
		// Get the value of an initialization parameter
	    String modernGeoDataSource = getServletConfig().getInitParameter("modernGeoDataSource");
		String histGeoDataSource = getServletConfig().getInitParameter("histGeoDataSource");
		String modernIndexDirectory = getServletConfig().getInitParameter("modernIndexDirectory");
		String histIndexDirectory = getServletConfig().getInitParameter("histIndexDirectory");
		
		try {
			// Build the indices
			GeoDataSource modernDataSource = GeoDataSource.createGeoNamesDataSource(modernGeoDataSource);
			GeoDataSource histDataSource = GeoDataSource.createPleiadesDataSource(histGeoDataSource);
			GeoIndexWriter geoIndexWriter = new GeoIndexWriter();
			Directory modernIndex = geoIndexWriter.buildIndexIfIndexNotExist(modernDataSource, modernIndexDirectory);
			Directory histIndex = geoIndexWriter.buildIndexIfIndexNotExist(histDataSource, histIndexDirectory);
			System.out.println("SERVLET STATUS: Finish building the indices");
			
			geo = new Georeference(modernIndex, histIndex);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("SERVLET STATUS: Finish initializing the service");
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		// Get the parameters
		String type = req.getParameter("type");
		String placeName = req.getParameter("placeName");
		String bound = req.getParameter("bound");
		String nearbyPlaces = req.getParameter("nearbyPlaces");
		String point = req.getParameter("point");
		String searchOptionStr = req.getParameter("searchOption");
		
		// Map the search options
		int searchOption = 0;
		if (searchOptionStr == null) {
			searchOption = Georeference.BOTH;
		}
		else if (searchOptionStr.equals("modern")) {
			searchOption = Georeference.MODERN_INDEX_ONLY;
		}
		else if (searchOptionStr.equals("historical")) {
			searchOption = Georeference.HIST_INDEX_ONLY;
		}
		else {
			searchOption = Georeference.BOTH;
		}
		
		// Perform the search
		String places = "";
		if (type == null || type.equals("match")) {
			try {
				places = geo.searchLocation(placeName, bound, nearbyPlaces, searchOption);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		else if (type.equals("nearby")) {
			try {
				places = geo.searchNearbyPlaces(placeName, point, searchOption);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		
		resp.getWriter().print(places);
		
	}

}
