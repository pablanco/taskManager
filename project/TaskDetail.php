<?php 
/***
 * Detail of product
 */

$win = new SDPanel();
$win -> setCaption("Task Detail");

$id   	  = new InputNumeric();
$name 	  = new InputText();
$status   = new InputBoolean();
$status->setLabelCaption("Status");
$status->setLabelPosition("Left");
$imagen 	= new InputImage();
//$name -> setClass("inputForm");

Data::getParm($id, $name, $status);

$table = new Table();
$table -> setClass("tablebackground");

$table_form = new Table();
$table_form -> setClass("tableforms");
$table_form -> setRowsStyle("76dip;76dip;76dip");
$table_form -> setHeight("90%");

$btn_save = new Button();
$btn_save -> setCaption("Save");
$btn_save -> setClass("buttonform");
$btn_save -> onTap(save());

$table_form -> addControl($id,1,1);
$table_form -> addControl($name,2,1);
$table_form -> addControl($imagen,3,1);
$table_form -> addControl($status,4,1);

$table_button = new Table();
$table_button -> addControl($btn_save,1,1);
$table_button -> setHeight("10%");

$table -> addControl($table_form,1,1);
$table -> addControl($table_button,2,1,1,1,"Center","Bottom");

$win -> addControl($table);
$win -> Render();

function refresh(){
	echo $name;
}

function start(){
	echo $id;
}

function save(){
	
	ProgressIndicator::Show();
	$request = new httpClient();	
	$request -> addVariable('taskID',$id);
	$request -> addVariable('status',$status);
	$request -> addVariable('task',$name);
	$request -> addVariable('imagen',$imagen);
	
	$result = $request -> Execute('POST',"http://52.34.247.215/frameWorkExamples/taskManager/crud/updateTask.php");
	
	$sdtError = array("response"=>type::Character(100));
	Data::FromJson($sdtError,$result);
	
	$rsValue = new InputText(100);
	$rsValue = $sdtError['response'];
	ProgressIndicator::Hide();
	$win -> Open("main");
	
	 
}


/*
function load_image(){
	$url = "http://www.devxtend.com/Gecko/magento/apiGecko/productos_imagen.php?pId=".$id;
	$httpClient = new httpClient();
	
	$result = $httpClient -> Execute('GET',$url);

	$str_images = array(
			array(
					"url"=>type::Character(350)
			)
	);
	
	Data::FromJson($str_images,$result);
	
	foreach ($str_images as $img){
		$image 	= $img['url'];
	}	
}

function call(){
	Interop::PlaceCall('099686088');
}

function email(){
	Interop::SendEmail('',$title,"Mensaje del correo, tienda online!");
}

function sms(){
	Interop::SendSMS('099696900','mensaje de texto desde tienda online..');
}

function view_image(){
	echo "Product ".$title;	
	$win -> Open("list_image_product",$id);
}

function add_cart(){
	$token = new InputText(80);
	$token = StorageAPI::Get("token");
		
	if($token != null){		
		ProgressIndicator::Show();
		$url_cart = "http://www.devxtend.com/Gecko/magento/apiGecko/clientes.php?metodo=addProductToCart&qty=1&productId=".$id."&customerToken=".$token;
		$hc = new httpClient();
		$rs_cart = $hc -> Execute("GET",$url_cart);
			
		$sdt_rs = array("error"=>type::Character(50));
		
		Data::FromJson($sdt_rs,$rs_cart);
				
		$rs = new InputText(50);
		$rs = $sdt_rs['error'];

		ProgressIndicator::Hide();
		
		if($rs == null){
			echo "Saved! ";
		}else{
			echo $rs;
		}
	}else{
		$win -> Open("Login");
	}	
}
*/
?>