package edu.cornell.georeference;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.function.CustomScoreProvider;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;

/**
 * This is a child class of the CustomerScoreProvider. It is determines how distance affects the score.
 * 
 * @author Yang Yang Zheng
 *
 */
public class DistanceScoreProvider extends CustomScoreProvider {
	
	private DistanceQueryBuilder dq;
	private int scoreOption;
	
	/**
	 * Create a distance score provider by providing the reader, distance query, and score option
	 * 
	 * @param reader 
	 * @param dq  the distance query builder that provides the distances
	 * @param scoreOption  controls how distance affects the score
	 */
	public DistanceScoreProvider(IndexReader reader, DistanceQueryBuilder dq, int scoreOption) {
		super(reader);
		this.dq = dq;
		this.scoreOption = scoreOption;
	}
	
	// Override
	// This function control how the distance impacts the score
	public float customScore(int doc, float subQueryScore, float valSrcScore) { 
		if (dq.getDistanceFilter().getDistance(doc) == null) {
			return 0; 
		}
		 
		double distance = dq.getDistanceFilter().getDistance(doc); 
		
		// Distance affects the score differently depending on the score option
		if (scoreOption == DistanceScoreQuery.LINEAR) {
			if (distance > 1.0) {
				return (float) (subQueryScore / distance);
			}
		}
		else if (scoreOption == DistanceScoreQuery.SQUARE_ROOT) {
			if (distance > 1.0) {
				return (float) (subQueryScore / Math.sqrt(distance));
			}
		}
		else if (scoreOption == DistanceScoreQuery.LOGARITHMIC) {
			if (distance > Math.E) {
				return (float) (subQueryScore / Math.log(distance));
			}
		}
		 
		return subQueryScore;
	}
}
