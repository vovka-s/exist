/*
	Copyright (c) 2004-2009, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["betterform.ui.select1.ComboBoxOpen"]){dojo._hasResource["betterform.ui.select1.ComboBoxOpen"]=true;dojo.provide("betterform.ui.select1.ComboBoxOpen");dojo.require("betterform.ui.ControlValue");dojo.require("dijit.form.ComboBox");dojo.declare("betterform.ui.select1.ComboBoxOpen",[betterform.ui.ControlValue,dijit.form.ComboBox],{options:null,postMixInProperties:function $DA4V_(){options=dojo.query("*[value]",this.srcNodeRef);this.inherited(arguments);this.applyProperties(dijit.byId(this.xfControlId),this.srcNodeRef);if(dojo.attr(this.srcNodeRef,"incremental")==undefined||dojo.attr(this.srcNodeRef,"incremental")==""||dojo.attr(this.srcNodeRef,"incremental")=="true"){this.incremental=true;}else{this.incremental=false;}},postCreate:function $DA4W_(){this.inherited(arguments);var _1=dojo.query("*[selected]",this.srcNodeRef)[0];if(_1!=undefined){var _2=dojo.attr(_1,"value");var _3=_1.innerHTML;this.setCurrentValue(_2);this.focusNode.value=_3;}else{this.setCurrentValue("");this.focusNode.value="";}},_onFocus:function $DA4X_(){this.inherited(arguments);this.handleOnFocus();},_onBlur:function $DA4Y_(){this.inherited(arguments);this.handleOnBlur();},getControlValue:function $DA4Z_(){var _4;var _5=this.focusNode.value;dojo.forEach(options,function(_6){if(_6.innerHTML==_5){_4=dojo.attr(_6,"value");}});if(_4!=undefined){return _4;}else{if(this.focusNode.value!=undefined){return this.focusNode.value;}else{return "";}}},onChange:function $DA4a_(_7){this.inherited(arguments);var _8;var _9=this.focusNode.value;dojo.forEach(options,function(_a){if(_a.innerHTML==_9){_8=_a;}});if(_8!=undefined){fluxProcessor.dispatchEventType(this.xfControl.id,"DOMActivate",dojo.attr(_8,"id"));}if(this.incremental){this.setControlValue();}},_handleSetControlValue:function $DA4b_(_b){var _c;dojo.forEach(options,function(_d){if(dojo.attr(_d,"value")==_b){_c=_d.innerHTML;}});if(_c!=undefined){this.focusNode.value=_c;}else{if(_b!=undefined){this.focusNode.value=_b;}else{this.focusNode.value="";}}}});}