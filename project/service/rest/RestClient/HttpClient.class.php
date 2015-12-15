<?php

/*
 * Copyright 2015 Gecko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


//http://codular.com/curl-with-php
//http://coreymaynard.com/blog/creating-a-restful-api-with-php/

class HttpClient{

    private $curl;
    private $error = null;

    protected static $_JSON_messages = array(
        JSON_ERROR_NONE => 'No error has occurred',
        JSON_ERROR_DEPTH => 'The maximum stack depth has been exceeded',
        JSON_ERROR_STATE_MISMATCH => 'Invalid or malformed JSON',
        JSON_ERROR_CTRL_CHAR => 'Control character error, possibly incorrectly encoded',
        JSON_ERROR_SYNTAX => 'Syntax error',
        JSON_ERROR_UTF8 => 'Malformed UTF-8 characters, possibly incorrectly encoded. You should use utf8_encode().'
    );

    public static function getInstance()
    {
        static $instance = null;
        if (null === $instance) {
            $instance = new static();
        }

        return $instance;
    }

    protected function __construct(){
        $this->curl = curl_init();
    }

    function __destruct(){
        curl_close($this->curl);
    }

    /**
     * Create a http request
     *
     * @access public
     * @since 1.0
     * @param string $method
     * @param string $url
     * @param array $data
     * @return array
     */

    public function getURL($method, $url, $data = false){
        
        $curl = $this->curl;

        switch ($method)
        {
            case "POST":
                curl_setopt($curl, CURLOPT_POST, 1);
                if ($data){
                    curl_setopt($curl, CURLOPT_POSTFIELDS, $this->_cleanInputs($data));
                }
                break;
            case "PUT":                
                curl_setopt($curl, CURLOPT_CUSTOMREQUEST, "PUT");
                if ($data){
                    curl_setopt($curl, CURLOPT_POSTFIELDS,http_build_query($this->_cleanInputs($data)));
                }
                break;
            case "DELETE":
                curl_setopt($curl, CURLOPT_CUSTOMREQUEST, "DELETE");
                if ($data){
                    curl_setopt($curl, CURLOPT_POSTFIELDS, $this->_cleanInputs($data));
                }
                break;
            case "GET":
                $url = sprintf("%s?%s", $url, http_build_query($this->_cleanInputs($data)));                
                break;
                      
            default:
                $this->setError('Invalid Method', 405);                
                break;
        }

        // Optional Authentication:
        //curl_setopt($curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
        //curl_setopt($curl, CURLOPT_USERPWD, "username:password");
        if(is_null($this->getError())){

            curl_setopt($curl, CURLOPT_URL, $url);
            curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
            $result = curl_exec($curl);    

            if ($result === false){
                $info = curl_getinfo($curl);
                $this->setError('Error: "' . curl_error($curl) . '" - Code: ' . curl_errno($curl));
                //' Error occured during curl exec ' . var_export($info)
                return false;
            }else{
                
                return $result;
            }
        }
    }

    public function getHeaders($url){
        
        // Create a curl handle
        $ch = $this->curl;
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_HEADER, true);    // we want headers

        // Execute
        curl_exec($ch);

        // Check if any error occurred
        if(!curl_errno($ch)){
             $info = curl_getinfo($ch);
             $httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);             
             return /*'Took ' . $info['total_time'] . ' seconds to send a request to ' . $info['url'] . ' with response code '.$httpcode.*/$info;
             
        }

         //Close handle
        //curl_close($ch);
    }

    public function setHeaders($headers){
        /*
        $headers = array();
        $headers[] = 'X-Apple-Tz: 0';
        $headers[] = 'X-Apple-Store-Front: 143444,12';
        $headers[] = 'Accept: text/html,application/xhtml+xml,application/xml';
        $headers[] = 'Accept-Encoding: gzip, deflate';
        $headers[] = 'Accept-Language: en-US,en;q=0.5';
        $headers[] = 'Cache-Control: no-cache';
        $headers[] = 'Content-Type: application/x-www-form-urlencoded; charset=utf-8';
        $headers[] = 'Host: www.example.com';
        $headers[] = 'Referer: http://www.example.com/index.php'; //Your referrer address
        $headers[] = 'User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:28.0) Gecko/20100101 Firefox/28.0';
        $headers[] = 'X-MicrosoftAjax: Delta=true';
        
		*/ 
		curl_setopt($this->curl, CURLOPT_HTTPHEADER, $headers);
        
    }


     /**
     * Recursive renameJsonKeys of json keys
     *
     * @access public
     * @since 1.0
     * @param array $matriz, original array
     * @param string $padre
     * @param array $merge, new key values
     * @return array
     */

    public function renameJsonKeys($matriz, $padre=null, $merge, &$array){

        if(!empty($matriz)){

            foreach($matriz as $key=>$value){
                if (is_array($value)){  //si es un array sigo recorriendo                                               
                    $this->renameJsonKeys($value,$key,$merge, $array);

                }else{  //si es un elemento lo cambio

                    if(!is_null($padre)){

                        if(array_key_exists($key, $merge)){                 
                            $array[$padre][$merge[$key]] = $value;  
                        }else{
                            $array[$padre][$key] = $value;  
                        }
                            
                    }else{

                        if(array_key_exists($key, $merge)){                 
                            $array[$merge[$key]] = $value;  
                        }else{
                            $array[$key] = $value;  
                        }

                    }

                    
                }

            }
            
        }

    }

    /**
     * Paginate json
     *
     * @access public
     * @since 1.0
     * @param int $start
     * @param int $count
     * @param array $result     
     */

    public function paginate($start, $count, &$result){
        if($start!='' && $count!=''){
            $result = array_slice($result, $start, $count, false);    
        }        
    }
    

    /**
     * Recursive search in json
     *
     * @access public
     * @since 1.0
     * @param array $array, original array
     * @param array $key, array of keys to find
     * @param data $value, value to find
     * @return array with elements found
     */

    public function search($array, $key, $value){
        $results = array();
        if(!empty($key) && $value!=''){            
            $this->search_r($array, $key, $value, $results);
            return $results;
        }else{
            return $array;
        }     
        
    }

    public function search_r($array, $key, $value, &$results){
        if (!is_array($array)) {
            return;
        }

        foreach ($key as $keyIndex=>$keyValue) {

            if (isset($array[$keyValue]) && !in_array($array, $results) ){
                
                $encuentra = strpos(strtolower($array[$keyValue]), strtolower($value));
                if ((strtolower($array[$keyValue]) == strtolower($value) || $encuentra!==false)) {
                    $results[] = $array;
                }

            }
        }
        foreach ($array as $subarray) {
            $this->search_r($subarray, $key, $value, $results);
        }
    }

    private function _cleanInputs($data) {
        $clean_input = Array();
        if (is_array($data)) {
            foreach ($data as $k => $v) {
                $clean_input[$k] = $this->_cleanInputs($v);
            }
        } else {
            $clean_input = trim(strip_tags($data));
        }
        return $clean_input;
    }

    public function setError($errorMsg){
        $this->error = $errorMsg;
    }

    public function getError(){
        return $this->error;
    }

    /**
     * String Json decode
     *
     * @access public
     * @since 1.0
     * @param string $string, string to decode     
     * @return string decoded
     */

    public function jsonDecode(&$string)
    {
        $string = json_decode($string, true);
        if($string){
            return;
        }
        echo __FUNCTION__.">".static::$_JSON_messages[json_last_error()];
        
    }


    /**
     * String Json encode
     *
     * @access public
     * @since 1.0
     * @param string $string, string to encode     
     * @return string decoded
     */

    public function jsonEncode(&$string){
        $string = json_encode($string, true);
        $string = str_replace("\\","", $string);

        if($string){
            return;
        }
        echo __FUNCTION__.">".static::$_JSON_messages[json_last_error()];
    }


}


function recorridaRecursiva($matriz, $padre=null, $GXDynPropConditions, $GXDynPropActions, &$array, $ultimo){

        if(!empty($matriz)){

            foreach($matriz as $key=>$value){
                if (is_array($value)){  //si es un array sigo recorriendo     
                    $condiciones = array();
                    recorridaRecursiva($value, $key, $GXDynPropConditions, $GXDynPropActions, $array, darUltimaClave($value));

                }else{  //si es un elemento lo cambio                   

                    if(!is_null($padre)){

                        $array[$padre][$key] = $value;                      
                        foreach($GXDynPropConditions as $GXDPkey => $GXDPvalue) {

                            if(array_key_exists($key, $GXDPvalue)){

                                echo "<br> va a evaluar en ".$GXDPkey." ".$key." ".$value." ".$GXDPvalue[$key]['logic_operator']." ".$value.$GXDPvalue[$key]['operator'].$GXDPvalue[$key]['value'];

                                $condiciones[] = array(
                                                        'oper' => $GXDPvalue[$key]['logic_operator'],
                                                        'cond' => (stringEvaluator($value,$GXDPvalue[$key]['value'],$GXDPvalue[$key]['operator'])==1)?1:0 
                                                );
                            }

                            if($ultimo==$key && !empty($condiciones)){

                                if(conditionEvaluator($condiciones)==1){
                                    $array[$padre]['Gxdynprop'] = $GXDynPropActions[$GXDPkey];  
                                }else{
                                    $array[$padre]['Gxdynprop'] = array();
                                }
                                
                                $condiciones = array();
                            }

                            echo "<br>*** termino de correr".$GXDPkey;

                        }


                            
                    }
                    
                }

            }
            
        }

    }

    function darUltimaClave($array){
        end($array);
        return key($array);
    }

    
    function conditionEvaluator($condition){ //Sirve para evaluar las condiciones generadas recursivamente

        $aux = '';
        foreach ($condition as $key => $value) {
            //si el operador logico no es nulo, es porque hay un valor anterior, tomo ese valor + el op logico y el valor actual y uso stringEvaluator
            if(!is_null($value['oper'])){
                $aux=(stringEvaluator($aux,intval($value['cond']),$value['oper'])==1)?1:0;
            }else{
                $aux=intval($value['cond']);
            }           
                        
        }
        return $aux;
        
    }


    function stringEvaluator($val_1,$val_2,$operator){ //Sirve para evaluar cada condicion individualmente
        
        $value = false;

        switch ($operator) {
            case '==':
                $value = ($val_1 == $val_2);
                break;
            
            case '!=':
                $value = ($val_1 != $val_2);
                break;

            case '>':
                $value = ( $val_1 > $val_2);
                break;

            case '<':
                $value = ( $val_1 < $val_2);
                break;
            case '&&':
                $value = ( $val_1 && $val_2);
                break;  
            case '||':
                $value = ( $val_1 || $val_2);
                break;
        }

        return intval($value);

    }


?>