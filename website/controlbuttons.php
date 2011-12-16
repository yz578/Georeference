<form name="controlButtonForm">
<table id="buttonTable">
<tr>
<td>
<input type ="button" class="groovybutton" name="backButton" value ="back"
	onClick="history.go(-1);return true;"
	onMouseOver="goLite(this.form.name,this.name)"
	onMouseOut="goDim(this.form.name,this.name)" />
</td>
<td>
<input type ="button" class="groovybutton" name="newSearchButton" value ="New Search"
	onClick="window.location='index.php';"
	onMouseOver="goLite(this.form.name,this.name)"
	onMouseOut="goDim(this.form.name,this.name)" />
</td>
</tr>
</table>
</form>