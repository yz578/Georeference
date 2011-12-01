<?php require_once "header.php"; ?>

<?php

// Change this url to the url of the web service
$baseURL = "http://localhost:8080/GeoreferenceWeb/geosearch";

$bound = "";
if (isset($_GET['boundPoint1Lat']) && isset($_GET['boundPoint1Lng']) && isset($_GET['boundPoint2Lat']) && isset($_GET['boundPoint2Lng'])) {
	if ($_GET['boundPoint1Lat'] != "" && $_GET['boundPoint1Lng'] != "" && $_GET['boundPoint2Lat'] != "" && $_GET['boundPoint2Lng'] != "") {
		$bound = $_GET['boundPoint1Lat'] . ',' . $_GET['boundPoint1Lng'] . '/' . $_GET['boundPoint2Lat'] . ',' . $_GET['boundPoint2Lng'];
	}
}

$nearbyPlaces = "";
if (isset($_GET['nearbyPointCount'])) {
	for ($i = 1; $i < $_GET['nearbyPointCount']+1; $i++) {
		if (isset($_GET['nearbyPoint' . $i . 'Lat']) && isset($_GET['nearbyPoint' . $i . 'Lng']) && isset($_GET['nearbyPoint' . $i . 'Rng'])) {
			if ($_GET['nearbyPoint' . $i . 'Lat'] != "" && $_GET['nearbyPoint' . $i . 'Lng'] != "" && $_GET['nearbyPoint' . $i . 'Rng'] != "") {
				if ($nearbyPlaces == "") {
					$nearbyPlaces = $_GET['nearbyPoint' . $i . 'Lat'] . ',' . $_GET['nearbyPoint' . $i . 'Lng'] . ',' . $_GET['nearbyPoint' . $i . 'Rng'];
				}
				else {
					$nearbyPlaces = $nearbyPlaces . '/' . $_GET['nearbyPoint' . $i . 'Lat'] . ',' . $_GET['nearbyPoint' . $i . 'Lng'] . ',' . $_GET['nearbyPoint' . $i . 'Rng'];
				}
			}
		}
	}
}

$url = $baseURL . "?placeName=$_GET[placeName]&bound=$bound&nearbyPlaces=$nearbyPlaces";

//echo $url;
$response = file_get_contents($url);

$results = new SimpleXMLElement($response);

echo <<<EOF
<table id="resultTable">
        <tr>
                <th scope="col">Place Name</th>
                <th scope="col">Coordinate</th>
                <th scope="col">Score</th>
        </tr>

EOF;
foreach($results as $result) {
        echo <<<EOF
        <tr>
                <td>{$result->place_name}</td>
				<td>{$result->latitude}, {$result->longitude}</td>
				<td>{$result->score}</td>
        </tr>

EOF;
}
echo '</table>';

?>
<a id="backLink" href="index.php">Back</a>

<br />
<br />
<br />
</div>
</body>
</html>