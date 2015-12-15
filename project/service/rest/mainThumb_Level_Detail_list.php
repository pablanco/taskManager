<?php

/**
* Rest client services.
* @author Gecko PHP Generator
* @version 1.0 
*/

require 'RestClient/HttpClient.class.php';

$url= "http://52.34.247.215/frameWorkExamples/taskManager/crud/getTask.php";

$data = array();
$merge = array("ID"=>"id","TASK"=>"name");

$searchValue = isset($_GET['Searchtext'])?$_GET['Searchtext']:"";
$start = isset($_GET['start'])?$_GET['start']:"";
$count = isset($_GET['count'])?$_GET['count']:"";

$cliente = HttpClient::getInstance();
$response = $cliente->getURL("GET", $url, $data);

if ($response == '{}' || $response == null || $response == '[]')
	$response = '{"empty":""}';

$cliente->jsonDecode($response);
$cliente->paginate($start, $count, $response);

$result = array();
$cliente->renameJsonKeys($response, $padre=null, $merge, $result);

/*
*/

$searchKey   = array(0=>"name");
$result = $cliente->search($result, $searchKey, $searchValue);

$cliente->jsonEncode($result);
echo $result;
?>
