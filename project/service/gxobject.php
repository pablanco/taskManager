<?php

/**
* Rest client services.
* @author Gecko PHP Generator
* @version 1.0 
*/

define('_BLOB_PATH', "http://52.34.247.215/frameWorkExamples/taskManager/service/");
define('_FOLDER',"PublicStorage/");

$fileName = getName() .".".getExtension(getHeader("Content-Type"));

fwrite_stream(_FOLDER.$fileName,file_get_contents( 'php://input' ));

echo json_encode(getResponse($fileName));

function getResponse($fileName){
	header('Content-Type: application/json; charset=utf-8');
	header('HTTP/ 201 ');
	header('GeneXus-Object-Id:  gxupload:'._FOLDER.$fileName);

	$data = array('object_id'=>_FOLDER.$fileName);
	return $data;
}

function getHeader($header){
	foreach (getallheaders() as $name => $value) {
		if($name == $header){
			return $value;
		}
	}
}

function fwrite_stream($path, $string) {
	$fp = fopen($path, 'w');
	$fwrite = fwrite($fp, $string);
}

function getExtension($type){

	if($type == "image/jpeg"){
		return "jpg";
	}

	if($type == "image/png"){
		return "png";
	}

	if($type == "video/mpeg"){
		return "mpg";
	}

	if($type == "video/quicktime"){
		return "mov";
	}

	if($type == "audio/mpeg"){
		return "mp3";
	}

	return "tmp";
}

function getName(){
	$str = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_';
	$shuffled = str_shuffle($str);

	return $shuffled;
}

?>