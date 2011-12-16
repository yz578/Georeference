<?php require_once "header.php"; ?>


<br />
<br />
<label id="resultListLabel">
Nearby places for "<?php echo $_GET['placeName']; ?> (<?php echo $_GET['latitude'] . "," . $_GET['longitude']; ?>)":
</label>

<?php

// Change this url to the url of the web service
$baseURL = "http://localhost:8080/GeoreferenceWeb/geosearch";

// Change this to change the range (max distance of the nearby places
$defaultRange = 5; 

$placeName = str_replace(" ", "%20", $_GET['placeName']);
$url = $baseURL . "?type=nearby&placeName=$placeName&point=$_GET[latitude],$_GET[longitude]," . $defaultRange;

$response = file_get_contents($url);

$results = new SimpleXMLElement($response);

echo <<<EOF
<table id="resultTable">
        <tr>
                <th scope="col">Place Name</th>
                <th scope="col">Coordinate</th>
        </tr>

EOF;
foreach($results as $result) {
        echo <<<EOF
        <tr>
                <td>{$result->place_name}</td>
				<td>{$result->latitude}, {$result->longitude}</td>
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