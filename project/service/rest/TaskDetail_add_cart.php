<?php

/**
* Rest client services.
* @author Gecko PHP Generator
* @version 1.0 
*/

require 'RestClient/HttpClient.class.php';

$url= "http://www.devxtend.com/Gecko/magento/apiGecko/clientes.php";

$inputJSON = file_get_contents('php://input');
$input= json_decode( $inputJSON, TRUE ); //convert JSON into array

$data = array("metodo"=>"addProductToCart","qty"=>"1","productId"=>$input["id"],"customerToken"=>$input["token"]);
$merge = array("error"=>"rs");

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

$cliente->jsonEncode($result);
echo $result;
?>
