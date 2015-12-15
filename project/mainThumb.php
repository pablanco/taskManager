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
$win -> setCaption("Tasks list thumb");


//Add button to action bar
$bar = new ActionBar();
$btn_img = new ButtonBar();
$btn_img -> setCaption("");
$btn_img -> setImage("img/ico_thumb.png");
$btn_img -> onTap(changeViewType());
$bar -> addControl($btn_img);
$win -> addControl($bar);


$table = new Table();
$table -> setClass("tableMenu");


$list = new Grid();
$list -> addData(load_grid());
$list -> addSearch($name);

$table_grid = new Canvas();
$table_grid -> setClass("tableTableDetailMain");

$id = new InputNumeric();
$name = new InputText();
$name -> setClass("inputTitle");
/*
$status   = new InputBoolean();
$status->setLabelCaption("Status");
$status->setLabelPosition("Left");
*/
$statusLabel = new Label();
$statusLabel->setCaption("Done");
$statusLabel->setClass("inputcommon");
$statusLabel -> onTap(changeStatus());
/*
$statusButton = new ButtonBar();
$statusButton -> setCaption("change");
//$statusButton -> setImage("img/ico_thumb.png");
$statusButton -> setClass("buttonform");
$statusButton -> onTap(changeViewType());
*/
$table_list = new Table();
$table_list -> addControl($name,1,1,1,4,"Left","Middle");
$table_list -> addControl($statusLabel,1,2,1,1,"Right","Middle");
//$table_list -> addControl($status,1,3,1,1,"Right","Middle");
//$table_list -> addControl($statusButton,1,4,1,1,"Right","Middle");

$table_grid -> addPosition($table_list,"0%","100%","0","50dip","0%","100%");
$table_grid -> onTap(detail());
$table_grid -> onLongTap(delete());

$list -> addControl($table_grid);
$table-> addControl($list);
$win -> addControl($table);
$win -> Render();

function load_grid(){
		
	//Make JSON request	
	$url = "http://52.34.247.215/frameWorkExamples/taskManager/crud/getTask.php";
	$httpClient = new httpClient();
	$result = $httpClient -> Execute('GET' ,$url);

	//Cast response data type
	$struct = array(
		array(
			"ID" 	 => type::Numeric(6),
			"TASK" 	 => type::Character(150),
			"STATUS" => type::Numeric(1)
		)
	);

	Data::FromJson($struct,$result);
	
	
	//Add result to screen vars 
	foreach ($struct as $items){
		$id 	= $items['ID'];
		$name 	= $items['TASK'];
		$status = $items['STATUS'];
	}	
}

function changeViewType(){
	//$win -> Open("ListadoThumb",$cat);
	echo "cambia";
}

function changeStatus(){
	//$win -> Open("ListadoThumb",$cat);
	echo "changeStatus";
}

function detail(){
	$win -> Open("TaskDetail",$id, $name, $status);
}

function delete(){
	$isOk = new InputBoolean();
	$isOk = Interop::Confirm("Do you want to delete this task ?");
	$urlDelete = "http://52.34.247.215/frameWorkExamples/taskManager/crud/deleteTask.php?taskID=".$id;
	
	if($isOk == true){
		
		$httpClient = new httpClient();
		$result = $httpClient -> Execute('GET' ,$urlDelete);
		
		$sdtError = array("response"=>type::Character(100));
		Data::FromJson($sdtError,$result);
		
		$rsValue = new InputText(100);
		$rsValue = $sdtError['response'];
		
		//$win -> setCaption("Guarda");
		
		echo $rsValue;
		$win -> Refresh();
		
		//return;
	}
}


?>