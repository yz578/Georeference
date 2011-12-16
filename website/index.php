<?php require_once "header.php"; ?>


<script type="text/javascript" defer ="defer">

// Allow the user to add nearby points to the form

var nearbyPointCount = 0;

function addNearbyPoint() {
	
	nearbyPointCount++;

	var table = document.getElementById('nearbyPoints');
	var rowCount = table.rows.length;
	var row = table.insertRow(rowCount);
	
	var cell1 = row.insertCell(0);
	cell1.width = 100;
	cell1.innerHTML = 'Point ' + nearbyPointCount;
	
	var newName = 'nearbyPoint' + nearbyPointCount + 'Lat';
	var cell2 = row.insertCell(1);
	cell2.width = 150;
	cell2.innerHTML = 'Latitude: <input name=\"' + newName + '\" id=\"' + newName + '\" class=\"pointField\" type =\"text\" />';
 
	newName = 'nearbyPoint' + nearbyPointCount + 'Lng';
	var cell3 = row.insertCell(2);
	cell3.width = 150;
	cell3.innerHTML += 'Longitude: <input name=\"' + newName + '\" id=\"' + newName + '\" class=\"pointField\" type =\"text\" />';
	
	newName = 'nearbyPoint' + nearbyPointCount + 'Rng';
	var cell4 = row.insertCell(3);
	cell4.width = 150;
	cell4.innerHTML = 'Range (mile): <input name=\"' + newName + '\" id=\"' + newName + '\" class=\"pointField\" type =\"text\" />';
	
	newName = 'delete' + nearbyPointCount;
	var cell5 = row.insertCell(4);
	cell5.innerHTML = '<img name=\"' + newName + '\" type=\"image\" src=\"delete.png\" class=\"deletePointButton\" onclick=\"deleteNearbyPoint(' + nearbyPointCount + ');\" />';

	var nearbyPointCountField = document.getElementById('nearbyPointCount');
	nearbyPointCountField.value = nearbyPointCount;
}

function deleteNearbyPoint(pointNumber) {
	var table = document.getElementById('nearbyPoints');
	var rowCount = table.rows.length;
	for(var i=0; i<rowCount; i++) {
		var row = table.rows[i];
		var point = row.cells[0].innerHTML;
		if(point == 'Point ' + pointNumber) {
			table.deleteRow(i);
			break;
		}
	}
}


// Check to make sure the values in the form are legal
function checkForm() {
	
	var placeName = document.getElementById("placeName");
	
	if (placeName.value == ""){
		alert ("Please enter the name of the location.");
		return false;
	}
		
	var decimalRegExp = new RegExp(/^[-+]?[0-9]+(\.[0-9]+)?$/);
	
	var boundLat1 = document.getElementById("boundPoint1Lat").value;
	var boundLat2 = document.getElementById("boundPoint2Lat").value;
	var boundLng1 = document.getElementById("boundPoint1Lng").value;
	var boundLng2 = document.getElementById("boundPoint2Lng").value;
	
	var bt1 = boundLat1 != "" && !decimalRegExp.test(boundLat1);
	var bt2 = boundLat2 != "" && !decimalRegExp.test(boundLat2);
	var bt3 = boundLng1 != "" && !decimalRegExp.test(boundLng1);
	var bt4 = boundLng2 != "" && !decimalRegExp.test(boundLng2);
	
	if (bt1 || bt2 || bt3 || bt4) {
		alert ("Invalid values for bound."); 
		return false;
	}
	
	var badNearbyPoints = false;
	for(var i = 0; i < nearbyPointCount; i++) {
		
		var baseID = 'nearbyPoint' + i;
		var lat = document.getElementById(baseID + 'Lat');
		var lng = document.getElementById(baseID + 'Lng');
		var rng = document.getElementById(baseID + 'Rng');
		
		var nt1 = lat != null && lat.value != "" && !decimalRegExp.test(lat.value);
		var nt2 = lng != null && lng.value != "" && !decimalRegExp.test(lng.value);
		var nt3 = rng != null && rng.value != "" && !decimalRegExp.test(rng.value);
		
		if (nt1 || nt2 || nt3) {
			badNearbyPoints = true;
			break;
		}
	}
	
	if (badNearbyPoints) {
		alert ("Invalid values for nearby points."); 
		return false;
	}
	
}

</script>

<br /><br />

<form id="geoSearchForm" name="geoSearchForm" method="get" action="geosearch.php" onsubmit="return checkForm();">
<div class="formBigLabel">Place Name</div>
<input name="placeName" id="placeName" type ="text" size="45" />
<br />
<br />
<div class="formBigLabel">Bound</div>
<table style="margin-left:10px;">
<tr>
<td width="100px">
Point 1
</td>
<td width="150px">
Latitude: <input name="boundPoint1Lat" id="boundPoint1Lat" class="pointField" type ="text" />
</td>
<td width="150px">
Longtidue: <input name="boundPoint1Lng" id="boundPoint1Lng" class="pointField" type ="text" />
</td>
</tr>
<tr>
<td width="100px">
Point 2
</td>
<td width="150px">
Latitude: <input name="boundPoint2Lat" id="boundPoint2Lat" class="pointField" type ="text" />
</td>
<td width="150px">
Longtidue: <input name="boundPoint2Lng" id="boundPoint2Lng" class="pointField" type ="text" />
</td>
</tr>
</table>
<br />
<div class="formBigLabel">Nearby Places</div>

<input type="hidden" name="nearbyPointCount" id="nearbyPointCount" value="0">

<div>
</div>

<table id="nearbyPoints" style="margin-left:10px;">

</table>
<input type ="button" name="addNearbyPointButton" class="groovybutton" value ="Add Point"
	onclick="addNearbyPoint();" 
	onMouseOver="goLite(this.form.name,this.name)"
	onMouseOut="goDim(this.form.name,this.name)" />
<br />
<br />
<div class="formBigLabel">Search Through</div>
<input type="radio" name="searchOption" value="modern">Modern Locations<br>
<input type="radio" name="searchOption" value="historical">Historical Locations<br>
<input type="radio" name="searchOption" value="both" checked>Both<br>
<br />
<br />
<input type ="submit" id="searchButton" name="searchButton" class="groovybutton" value ="Search" 
	onMouseOver="goLite(this.form.name,this.name);"
	onMouseOut="goDim(this.form.name,this.name);" />
</form>

</div>
</body>
</html>