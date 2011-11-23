package edu.cornell.georeference;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.lucene.store.Directory;


@WebServlet("/georeference")
public class GeoreferenceServlet extends HttpServlet {
	
	Directory modernIndex;
	Directory histIndex;
	
	public void init() {
		// Get the value of an initialization parameter
	    String modernGeoDataSource = getServletConfig().getInitParameter("modernGeoDataSource");
		String histGeoDataSource = getServletConfig().getInitParameter("histGeoDataSource");
		String modernIndexDirectory = getServletConfig().getInitParameter("modernIndexDirectory");
		String histIndexDirectory = getServletConfig().getInitParameter("histIndexDirectory");
		
		try {
			GeoIndexWriter geoIndexWriter = new GeoIndexWriter();
			modernIndex = geoIndexWriter.getGeonameDirectory(modernGeoDataSource, modernIndexDirectory);
			histIndex = geoIndexWriter.getHistnameDirectory(histGeoDataSource, histIndexDirectory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		String placeName = req.getParameter("placeName");
		String bound = req.getParameter("bound");
		String nearbyPlaces = req.getParameter("nearbyPlaces");
		if (placeName != null) {
			placeName = placeName.toLowerCase();
		}
		
		Georeference geo = new Georeference(false);
		String places = geo.searchLocation(modernIndex, histIndex, placeName, bound, nearbyPlaces);
		
		resp.getWriter().print(places);
		
	}

}
