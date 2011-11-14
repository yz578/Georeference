package georeference;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.lucene.store.Directory;


@WebServlet("/georeference")
public class GeoreferenceServlet extends HttpServlet {
	
	Directory modernIndex;
	Directory histIndex;
	
	public GeoreferenceServlet() throws IOException {
		super();
		GeoIndexWriter geoIndexWriter = new GeoIndexWriter();
		modernIndex = geoIndexWriter.getGeonameDirectory(
				"C:/Users/Yang Yang/workspace/GeoreferenceWeb2/data/cities1000.txt",
				"C:/Users/Yang Yang/workspace/GeoreferenceWeb2/index/geoname");
		histIndex = geoIndexWriter.getHistnameDirectory(
				"C:/Users/Yang Yang/workspace/GeoreferenceWeb2/data/pleiades-names.txt",
				"C:/Users/Yang Yang/workspace/GeoreferenceWeb2/index/histname");
		System.out.println("Finish getting indices");
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		String placeName = req.getParameter("placeName");
		String bound = req.getParameter("bound");
		String nearbyPlaces = req.getParameter("nearbyPlaces");
		if (placeName != null) {
			placeName = placeName.toLowerCase();
		}
		
		Georeference geo = new Georeference();
		String places = geo.searchLocation(modernIndex, histIndex, placeName, bound, nearbyPlaces);
		
		resp.getWriter().print(places);
		
	}

}
