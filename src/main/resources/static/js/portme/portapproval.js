//start code for paralel request approval
$(document).on("click", ".portApproval", function() {
	// Getting data from request
	var requestId = $(this).data('id');
	var msisdn = $(this).data('title');
	var billingUID = document.getElementById('billingUID').value
	var instanceId = document.getElementById('instanceId').value
	// store data in json object
	var jsonData = {
		"requestId" : requestId,
		"approval" : "Yes",
		"subscriberArrType" : [ {
			"msisdn" : msisdn
		} ],
		"comment" : "success",
		"billingUID1" : billingUID,
        "instanceID" : instanceId
	}
	var form = new FormData();
	form.append("portMeApproval", JSON.stringify(jsonData));
	fire_ajax_submit(form);
	$(this).closest('tr').remove();
});

// start code for whole request approval
$(document).on("click", ".portAllApproval", function() {
	// Getting data from request
	//var requestId = $(this).data('id');
	var requestId = document.getElementById('aprovalRequestId').value
	var selectedValue = document.getElementById('actionTaken').value;
	var selectReason = document.getElementById('selectReason').value;
	var billingUID = document.getElementById('billingUID').value
	var instanceId = document.getElementById('instanceId').value

	// store data in json object
	var jsonData = {
		"requestId" : requestId,
		"approval" : selectedValue,
		"comment" : selectReason,
        "billingUID1" : billingUID,
        "instanceID" : instanceId
	}
	var form = new FormData();
	form.append("portMeApproval", JSON.stringify(jsonData));
	fire_ajax_submit(form);
});


function fire_ajax_submit(form){
	$.ajax({
		type : "POST",
		url : "api/portapproval",
		success : function(result) {
			var response = JSON.parse(result);
			alert(response.responseMessage);
		},
		error : function(error) {
			console.log(error);
		},
		async : true,
		data : form,
		cache : false,
		contentType : false,
		processData : false,
		timeout : 60000
	});

};
