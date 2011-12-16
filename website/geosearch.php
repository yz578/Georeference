<?php require_once "header.php"; ?>


<br />
<br />
<label id="resultListLabel">
Search results for "<?php echo $_GET['placeName']; ?>":
</label>

<?php

// Change this url to the url of the web service
$baseURL = "http://localhost:8080/GeoreferenceWeb/geosearch";

// Get the bound
$bound = "";
if (isset($_GET['boundPoint1Lat']) && isset($_GET['boundPoint1Lng']) && isset($_GET['boundPoint2Lat']) && isset($_GET['boundPoint2Lng'])) {
	if ($_GET['boundPoint1Lat'] != "" && $_GET['boundPoint1Lng'] != "" && $_GET['boundPoint2Lat'] != "" && $_GET['boundPoint2Lng'] != "") {
		$bound = $_GET['boundPoint1Lat'] . ',' . $_GET['boundPoint1Lng'] . ';' . $_GET['boundPoint2Lat'] . ',' . $_GET['boundPoint2Lng'];
	}
}

// Get nearby points
$nearbyPlaces = "";
if (isset($_GET['nearbyPointCount'])) {
	for ($i = 1; $i < $_GET['nearbyPointCount']+1; $i++) {
		if (isset($_GET['nearbyPoint' . $i . 'Lat']) && isset($_GET['nearbyPoint' . $i . 'Lng']) && isset($_GET['nearbyPoint' . $i . 'Rng'])) {
			if ($_GET['nearbyPoint' . $i . 'Lat'] != "" && $_GET['nearbyPoint' . $i . 'Lng'] != "" && $_GET['nearbyPoint' . $i . 'Rng'] != "") {
				if ($nearbyPlaces == "") {
					$nearbyPlaces = $_GET['nearbyPoint' . $i . 'Lat'] . ',' . $_GET['nearbyPoint' . $i . 'Lng'] . ',' . $_GET['nearbyPoint' . $i . 'Rng'];
				}
				else {
					$nearbyPlaces = $nearbyPlaces . ';' . $_GET['nearbyPoint' . $i . 'Lat'] . ',' . $_GET['nearbyPoint' . $i . 'Lng'] . ',' . $_GET['nearbyPoint' . $i . 'Rng'];
				}
			}
		}
	}
}

$placeName = str_replace(" ", "%20", $_GET['placeName']);
$url = $baseURL . "?type=match&searchOption=$_GET[searchOption]&placeName=$placeName&bound=$bound&nearbyPlaces=$nearbyPlaces";

$response = file_get_contents($url);

$results = new SimpleXMLElement($response);

echo <<<EOF
<table id="resultTable">
        <tr>
                <th scope="col">Place Name</th>
                <th scope="col">Coordinate</th>
                <th scope="col">Score</th>
				<th scope="col">Nearby Places</th>
        </tr>

EOF;
foreach($results as $result) {
        echo <<<EOF
        <tr>
                <td>{$result->place_name}</td>
				<td>{$result->latitude}, {$result->longitude}</td>
				<td>{$result->score}</td>
				<td><a class="nearbyLink" href="nearbyplaces.php?placeName={$result->place_name}&latitude={$result->latitude}&longitude={$result->longitude}">nearby places</a></td>
        </tr>

EOF;
}
echo '</table>';

?>

<?php require_once "controlbuttons.php"; ?>

<br />
<br />
<br />
</div>
</body>
</html>