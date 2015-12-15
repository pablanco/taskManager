<?php 
/**
 * Main object.
 * @author Gecko
 * @version 1.0
 */
/*
$win = new SDPanel();
$win -> setCaption("Task Manager");

$mainTable = new Table();

$button = new Button();
$button -> setCaption("Click me!");
$button -> onTap(clickme());

$mainTable -> addControl($button,1,1);
$win -> addControl($mainTable);

function clickme(){
	echo "Hello !";
}*/

/***
 * Listado de categorias.
 */

$win = new SDPanel();
$win -> setCaption("Tasks list");

$table = new Table();
$table -> setClass("table.Menu");


$table_home = new Table();
$table_home -> setClass("table.MenuOption");
$table_home -> setColumnsStyle("20%;80%");

$img_home = new Image();
$img_home -> setImage("img/ico_thumb.png");
$img_home -> setClass("ImageMenu");

$lbl_home = new Label();
$lbl_home -> setClass("LabelMenuOption");
$lbl_home -> setCaption("Home");

$table_home -> onTap(home());
//$table_home -> addControl($img_home,1,1,1,1,"Center","Middle");
$table_home -> addControl($lbl_home,1,2,1,1,"Left","Middle");

$table -> addControl($table_home,2,1);
$win -> addControl($table);
$win -> Render();


function Slide(){	
	$win -> Open("mainThumb");		
}

function home(){
	$win -> Open("mainThumb");
}




?>