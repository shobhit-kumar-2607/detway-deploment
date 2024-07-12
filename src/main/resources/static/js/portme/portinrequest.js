// start code for download sample file
var quoteData = [ [ '9810100001', '200101', '1001', '2002' ] ];
function download_quotefile() {
	var csv = 'msisdn,pincode,sim,imsi\n';
	quoteData.forEach(function(row) {
		csv += row.join(',');
		csv += "\n";
	});
	var hiddenElement = document.createElement('a');
	hiddenElement.href = 'data:text/csv;charset=utf-8,' + encodeURI(csv);
	hiddenElement.target = '_blank';
	hiddenElement.download = 'plannumber.csv';
	hiddenElement.click();
}

// start code for change input column for personal and corporate plan number
$(document).ready(function() {
	$('#dataType').on('change', function() {
		if (this.value == '1') {
			$("#corporate").hide();
			$("#personal").show();
			$("#personalPincde").show();
		} else {
			$("#personal").hide();
			$("#personalPincde").hide();
			$("#corporate").show();
		}
	});
});

/* start code for add new row in table */
$('document')
		.ready(
				function() {
					$('.add_another')
							.click(
									function() {
										$("#example1")
												.append(
														'<tr><td contenteditable="true"></td><td contenteditable="true"></td><td contenteditable="true"></td><td contenteditable="true"></td></tr>');
									});
				});

/* end code for add new row in table */
var bulkfile;
var docfile;
var fileSize = 0;
$(document)
		.ready(
				function() {
					uploadFiles()
					function uploadFiles() {
						var bulkUploader = $('.form-controls');
						bulkUploader.on('change', function() {
							bulkfile = bulkUploader[0].files[0];
						});
						var docUploader = $('.doc-control');
						docUploader
								.on(
										'change',
										function() {
											var myFile = "";
											myFile = docUploader.val();
											var upld = myFile.split('.').pop();
											if (upld == 'pdf') {
												fileSize = docUploader[0].files[0].size;
												fileSize = Math
														.floor(fileSize / 1048576);
												if (fileSize <= 20) {
													docfile = docUploader[0].files[0];
												} else {
													alert('Document is too large. Please upload less than 10mb.');
												}
											} else {
												alert("Only PDF are allowed");
											}
										})
					}
				})
/* start code to submit request into BACKEND */
var billingUID1 = document.getElementById("billingUID1")
var instanceID = document.getElementById("instanceID")
var rno = document.getElementById("rno")
var dno = document.getElementById("dno")
var area = document.getElementById("area")
var rn = document.getElementById("rn")
var companyCode = document.getElementById("companyCode")
var service = document.getElementById("service")
var dataType = document.getElementById("dataType")
var orderType = document.getElementById("orderType")
var partnerID = document.getElementById("partnerID")
var msisdn = document.getElementById("msisdn")
var owner = document.getElementById("owner")
var dummyMSISDN = document.getElementById("dummyMSISDN")
// var sim = document.getElementById("sim")
// var imsi = document.getElementById("imsi")
var hlr = document.getElementById("hlr")
// var pinCode = document.getElementById("pinCode")

var btnSubmit = document.getElementById("btnSubmit")

// add click event listener, to get data when data is entered
btnSubmit.addEventListener("click", function() {
	// GET data from table for corporate request
	var table = $('#example1').DataTable({
		"dom" : 'rtip',
		"bPaginate" : false,
		"bLengthChange" : false,
		"bFilter" : true,
		"bInfo" : false,
		"bAutoWidth" : false,
		"destroy" : true,
	});
	var data = table.rows().data();
	var arrayLength = data.length;
	var corporateData = [];
	var validateMSISDN;
	var flagCorpMSISDN = true;
	for (var i = 0; i < arrayLength; i++) {
		var col1 = data[i][0];
		var col2 = data[i][1];
		var col3 = data[i][2];
		var col4 = data[i][3];
		if (col1 === '') {
		} else {
			if (validateCorporateMSISDN(col1, col2) == true) {
				var dtCorporate = {
					"msisdn" : col1,
					"pinCode" : col2,
					"sim" : col3,
					"imsi" : col4,
					"dummyMSISDN" : dummyMSISDN.value,
					"hlr" : hlr.value
				};
				corporateData.push(dtCorporate);
			} else {
				flagCorpMSISDN = false;
			}
		}
	}
	if (prefixValidate() == true && flagCorpMSISDN === true) {
		// store data in json object
		var jsonDatas;
		if (dataType.value == 2) {
			jsonDatas = {
				"billingUID1" : billingUID1.value,
				"instanceID" : instanceID.value,
				"rno" : rno.value,
				"dno" : dno.value,
				"area" : area.value,
				"rn" : rn.value,
				"companyCode" : companyCode.value,
				"service" : service.value,
				"dataType" : dataType.value,
				"orderType" : orderType.value,
				"partnerID" : partnerID.value,
				"hlr" : hlr.value,
				"dummyMSISDN" : dummyMSISDN.value,
				"owner" : owner.value,
				"subscriberArrType" : corporateData,
				"customerData" : {
					"subscriberId" : "123456789",
					"remark1" : null,
					"remark2" : null,
					"remark3" : null,
					"remark4" : null,
					"remark5" : null
				}
			}
		} else {
			jsonDatas = {
				"billingUID1" : billingUID1.value,
				"instanceID" : instanceID.value,
				"rno" : rno.value,
				"dno" : dno.value,
				"area" : area.value,
				"rn" : rn.value,
				"companyCode" : companyCode.value,
				"service" : service.value,
				"dataType" : dataType.value,
				"orderType" : orderType.value,
				"partnerID" : partnerID.value,
				"hlr" : hlr.value,
				"owner" : owner.value,
				"dummyMSISDN" : dummyMSISDN.value,
				"subscriberSequence" : {
					"subscriberNumber" : msisdn.value
				},
				"personCustomer" : {
					"ownerName" : owner.value,
					"ownerId" : companyCode.value,
					"typeOfId" : 1,
					"signatureDate" : "2023-01-01"
				}
			}
		}
		// convert json data into formDat and send ajax request to application
		var form = new FormData();
		form.append("portme", JSON.stringify(jsonDatas));
		form.append("docFile", docfile);
		if (bulkfile === undefined) {
		} else {
			form.append("bulkUpload", bulkfile);
		}
		$('#loader').show();
		$.ajax({
			type : "POST",
			url : "api/initportrequest",
			success : function(result) {
				var response = JSON.parse(result);
				alert(response.responseMessage);
				if (response.responseCode == 200) {
					$("#btnSubmit").prop("disabled", true);
					$('#loader').hide();
				} else {
					$('#loader').hide();
				}
			},
			error : function(error) {
				console.log(error);
				$('#loader').hide();
			},
			async : true,
			data : form,
			cache : false,
			contentType : false,
			processData : false,
			timeout : 60000
		});
	}
});

// start shobhit
$("#area").on('change', function() {
	const area = this.value;
	var opId = document.getElementById("rno").value;
	$.ajax({
		type : "GET",
		url : "getrn?op_id=" + opId + "&area=" + area,
		success : function(result) {
			document.getElementById("rn").value = JSON.parse(result);
		},
		error : function(error) {
			console.log(error);
		},
		async : true,
		cache : false,
		contentType : false,
		processData : false,
		timeout : 60000
	});

});

$("#msisdn").on('change', function() {
	var msisdn = $("#msisdn").val();

	// Show loading indicator
	// $("#dno").val("0000");

	$.ajax({
		type : "GET",
		url : "getmsisdn",
		data : {
			msisdn : msisdn
		},
		success : function(result) {
			$("#dno").val(JSON.parse(result));
		},
		error : function(jqXHR, textStatus, errorThrown) {
			$("#dno").val("0000");
		},
		async : true,
		cache : false,
		contentType : "application/json",
		processData : true,
		timeout : 60000
	});
});
function prefixValidate() {
	// for msisdn
	document.getElementById("billingUIDlebel").style.visibility = "hidden";
	document.getElementById("instanceIDlebel").style.visibility = "hidden";
	document.getElementById("partnerIDlebel").style.visibility = "hidden";
	document.getElementById("hlrlebel").style.visibility = "hidden";
	document.getElementById("dummyMSISDNlebel").style.visibility = "hidden";
	document.getElementById("msisdnlebel").style.visibility = "hidden";
	document.getElementById("companylebel").style.visibility = "hidden";
	var result = true;
	if (hlr.value.trim() === "") {
		document.getElementById("hlrtext").textContent = "HLR can't be empty";
		document.getElementById("hlrlebel").style.visibility = "visible";
		result = false;
	} else if (hlr.value.length !== 6) {
		document.getElementById("hlrtext").textContent = "HLR should be 6 ALFA Numeric";
		document.getElementById("hlrlebel").style.visibility = "visible";
		result = false;
	}
	// Validate billingUID
	if (billingUID1.value.trim() === "") {
		document.getElementById("billingUIDtext").textContent = "Billing UID can't be empty";
		document.getElementById("billingUIDlebel").style.visibility = "visible";
		result = false;
	} else if (billingUID1.value.length !== 10) {
		document.getElementById("billingUIDtext").textContent = "Billing UID should be exactly 10 characters long";
		document.getElementById("billingUIDlebel").style.visibility = "visible";
		result = false;
	}

	// Validate instanceID
	if (instanceID.value.trim() === "") {
		document.getElementById("instanceIDtext").textContent = "Instance ID can't be empty";
		document.getElementById("instanceIDlebel").style.visibility = "visible";
		result = false;
	} else if (instanceID.value.length !== 10) {
		document.getElementById("instanceIDtext").textContent = "Instance ID should be exactly 10 characters long";
		document.getElementById("instanceIDlebel").style.visibility = "visible";
		result = false;
	}

	// Validate partnerID
	if (partnerID.value.trim() === "") {
		document.getElementById("partnerIDtext").textContent = "Partner ID can't be empty";
		document.getElementById("partnerIDlebel").style.visibility = "visible";
		result = false;
	} else if (partnerID.value.length !== 10) {
		document.getElementById("partnerIDtext").textContent = "Partner ID should be exactly 10 characters long";
		document.getElementById("partnerIDlebel").style.visibility = "visible";
		result = false;
	}// Validate MSISDN

	if (dataType.value == 1) {
		// Validate Company Code
		if (companyCode.value.trim() === "") {
			document.getElementById("companytext").textContent = "PinCode can't be empty";
			document.getElementById("companylebel").style.visibility = "visible";
			result = false;
		} else if (companyCode.value.length !== 8) {
			document.getElementById("companytext").textContent = "PinCode should be 8 ALFA Numeric ";
			document.getElementById("companylebel").style.visibility = "visible";
			result = false;
		}
		if (msisdn.value.trim() === "") {
			document.getElementById("msisdntext").textContent = "MSISDN can't be empty";
			document.getElementById("msisdnlebel").style.visibility = "visible";
			result = false;
		} else if (!/^\d+$/.test(msisdn.value)) {
			document.getElementById("msisdntext").textContent = "Please provide numerical values.";
			document.getElementById("msisdnlebel").style.visibility = "visible";
			result = false;
		} else if (msisdn.value.length !== 10) {
			document.getElementById("msisdntext").textContent = "MSISDN should be 10 digits only";
			document.getElementById("msisdnlebel").style.visibility = "visible";
			result = false;
		}
	}
	if (dataType.value == 2) {
		var fileInput = document.getElementById('uploadoc');
		// Check if the file input is empty
		if (!fileInput.files.length) {
			document.getElementById("uploadDocFiletext").textContent = "Upload Document cann't be empty";
			document.getElementById("uploadDocFilelbl").style.visibility = "visible";
		} else {
			document.getElementById("uploadDocFilelbl").style.visibility = "hidden";
		}
	}
	// Validate Alternate Number (dummyMSISDN)
	if (dummyMSISDN.value.trim() === "") {
		document.getElementById("dummyMSISDNtext").textContent = "Alternate Number can't be empty";
		document.getElementById("dummyMSISDNlebel").style.visibility = "visible";
		result = false;
	} else if (!/^\d+$/.test(dummyMSISDN.value)) {
		document.getElementById("dummyMSISDNtext").textContent = "Please provide numerical values.";
		document.getElementById("dummyMSISDNlebel").style.visibility = "visible";
		result = false;
	} else if (dummyMSISDN.value.length !== 10) {
		document.getElementById("dummyMSISDNtext").textContent = "Alternate Number should be 10 digits only";
		document.getElementById("dummyMSISDNlebel").style.visibility = "visible";
		result = false;
	}

	return result;
}

function validateCorporateMSISDN(msisdns, pincodes) {
	document.getElementById("corporatemsisdnlebel").style.visibility = "hidden";
	if (msisdns.trim() === "") {
		document.getElementById("corporatemsisdntext").textContent = "MSISDN can't be empty";
		document.getElementById("corporatemsisdnlebel").style.visibility = "visible";
		return false;
	} else if (!/^\d+$/.test(msisdns)) {
		document.getElementById("corporatemsisdntext").textContent = "Please provide numerical values.";
		document.getElementById("corporatemsisdnlebel").style.visibility = "visible";
		return false;
	} else if (msisdns.length !== 10) {
		document.getElementById("corporatemsisdntext").textContent = "MSISDN should be 10 digits only";
		document.getElementById("corporatemsisdnlebel").style.visibility = "visible";
		return false;
	} else if (pincodes.trim() === "") {
		document.getElementById("corporatemsisdntext").textContent = "PinCode can't be empty";
		document.getElementById("corporatemsisdnlebel").style.visibility = "visible";
		return false;
	} else if (pincodes.length !== 8) {
		document.getElementById("corporatemsisdntext").textContent = "PinCode should be 8 ALFA Numeric ";
		document.getElementById("corporatemsisdnlebel").style.visibility = "visible";
		return false;
	}
	return true;
}

// end shobhit
