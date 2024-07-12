//$(document).on("click", ".orderActivate", function() {
//	// Getting data from request
//	var requestId = $(this).data('id');
//	var msisdn = $(this).data('title');
//	var service = $(this).data('tile');
//	// store data in json object
//	var jsonData = {
//		"service" : service,
//		"msisdnUID" : [ {
//			"msisdn" : msisdn,
//			"requestId" : requestId
//		} ]
//	}
//	var form = new FormData();
//	form.append("portmeanswer", JSON.stringify(jsonData));
//
//	$.ajax({
//		type : "POST",
//		url : "api/connectionanswer",
//		success : function(result) {
//			var response = JSON.parse(result);
//			alert(response.responseMessage);
//		},
//		error : function(error) {
//			console.log(error);
//		},
//		async : true,
//		data : form,
//		cache : false,
//		contentType : false,
//		processData : false,
//		timeout : 60000
//	});
//	$(this).closest('tr').remove();
//});


$(document).on("click", "#btnSubmit", function () {
	document.getElementById('selectAllCheckbox').addEventListener('change', function () {
		var checkboxes = document.querySelectorAll('.rowCheckbox');
		checkboxes.forEach(function (checkbox) {
			checkbox.checked = this.checked;
		});
	});
	var msisdnData = [];
	var requestId, service;

	$(".rowCheckbox:checked").each(function () {
		var msisdn = $(this).data('msisdn');
		requestId = $(this).data('request-id'); // assuming all checked rows
												// have the same request ID
		service = $(this).data('service'); // assuming all checked rows have
											// the same service
		msisdnData.push(msisdn);
	});

	if (msisdnData.length === 0) {
		alert("Please select at least one MSISDN.");
		return;
	}

	// Create jsonData object
	var jsonData = {
		"service": service,
		"msisdnUID": msisdnData.map(function (msisdn) {
			return {
				"msisdn": msisdn,
				"requestId": requestId
			};
		})
	};

	var form = new FormData();
	form.append("portmeanswer", JSON.stringify(jsonData));

	$.ajax({
		type: "POST",
		url: "api/connectionanswer",
		success: function (result) {
			var response = JSON.parse(result);
			alert(response.responseMessage);
		},
		error: function (error) {
			console.log(error);
		},
		async: true,
		data: form,
		cache: false,
		contentType: false,
		processData: false,
		timeout: 60000
	});

	// Optionally remove checked rows after submission
	$(".rowCheckbox:checked").closest('tr').remove();
});

	document.addEventListener('DOMContentLoaded', function() {
	const selectAllCheckbox = document.getElementById('selectAllCheckbox');
	const rowCheckboxes = document.querySelectorAll('.rowCheckbox');
	const checkedIds = new Set();

	// Function to update the checked IDs
	function updateCheckedIds() {
	checkedIds.clear();
	rowCheckboxes.forEach(checkbox => {
	if (checkbox.checked) {
	checkedIds.add(checkbox.getAttribute('data-request-id'));
}
});
	console.log(Array.from(checkedIds));  // For debugging purposes
}

	// Event listener for the "Select All" checkbox
	selectAllCheckbox.addEventListener('change', function() {
	rowCheckboxes.forEach(checkbox => {
	checkbox.checked = this.checked;
});
	updateCheckedIds();
});

	// Event listeners for each row checkbox
	rowCheckboxes.forEach(checkbox => {
	checkbox.addEventListener('change', function() {
	if (!this.checked) {
	selectAllCheckbox.checked = false;
} else if (Array.from(rowCheckboxes).every(checkbox => checkbox.checked)) {
	selectAllCheckbox.checked = true;
}
	updateCheckedIds();
});
});
});
