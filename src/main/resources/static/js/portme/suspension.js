/* start code to submit request into BACKEND */
var billingUID1 = document.getElementById("billingUID1")
var accountID = document.getElementById("accountID")
var msisdn = document.getElementById("msisdn")
var billDate = document.getElementById("litepickerBillDate")
var billDueDate = document.getElementById("litepickerBillDueDate")
var amount = document.getElementById("amount")
var comment = document.getElementById("comment")

var susReqbtnSubmit = document.getElementById("susReqbtnSubmit");
var billcancelbtn = document.getElementById("billcancelbtn");
var susdnoackbtn = document.getElementById("susdnoackbtn");

var barringReqbtnSubmit = document.getElementById("barringReqbtnSubmit");
var barringcancelbtn = document.getElementById("barringcancelbtn");
var barringconfirmbtn = document.getElementById("barringconfirmbtn");

// add click event listener, to get data when data is entered
if (susReqbtnSubmit != null) {
	susReqbtnSubmit.addEventListener("click", function() {
		if (susDNORequest() == true) {
			var jsonDatas = {
				"billingUid" : billingUID1.value,
				"accountId" : accountID.value,
				"msisdn" : msisdn.value,
				"billDate" : billDate.value,
				"dueDate" : billDueDate.value,
				"amount" : amount.value,
				"comment" : comment.value,
				"reason" : ""
			}

			var form = new FormData();
			form.append("suspension", JSON.stringify(jsonDatas));
			$('#loader').show();
			$.ajax({
				type : "POST",
				url : "api/suspension-dno-request",
				success : function(result) {
					var response = JSON.parse(result);
					alert(response.responseMessage);
					if (response.responseCode == 200) {
						$("#susReqbtnSubmit").prop("disabled", true);
						$('#loader').hide();
					} else {
						$('#loader').hide();
					}
				},
				error : function(error) {
					$('#loader').hide();
					console.log(error);
				},
				async : true,
				data : form,
				cache : false,
				contentType : false,
				processData : false,
				timeout : 60000
			});
		}
	})
};

// start code for cancel bill request
if (billcancelbtn != null) {

	billcancelbtn.addEventListener("click", function() {
		var reason = document.getElementById("reason");
		var msisdns = document.getElementById("msisdn");
		var transactionId = document.getElementById("transactionId");
		var jsonDatas = {
			"requestId" : transactionId.value,
			"msisdn" : msisdns.value,
			"reason" : reason.value
		}
		var form = new FormData();
		form.append("suspension", JSON.stringify(jsonDatas));
		$('#loader').show();
		$.ajax({
			type : "POST",
			url : "api/suspension-dno-cancel",
			success : function(result) {
				var response = JSON.parse(result);
				alert(response.responseMessage);
				if (response.responseCode == 200) {
					$("#billcancelbtn").prop("disabled", true);
					$('#loader').hide();
				} else {
					$('#loader').hide();
				}
			},
			error : function(error) {
				$('#loader').hide();
				console.log(error);
			},
			async : true,
			data : form,
			cache : false,
			contentType : false,
			processData : false,
			timeout : 60000
		});
	});
};

// start code for Suspension DNO ACK request
if (susdnoackbtn != null) {

	susdnoackbtn.addEventListener("click", function() {
		var transactionId = document.getElementById("transactionId");
		var jsonDatas = {
			"requestId" : transactionId.value,
			"billingUid" : billingUID1.value,
			"accountId" : accountID.value,
			"msisdn" : msisdn.value
		}
		var form = new FormData();
		form.append("suspension", JSON.stringify(jsonDatas));
		$('#loader').show();
		$.ajax({
			type : "POST",
			url : "api/suspension-dno-ack",
			success : function(result) {
				var response = JSON.parse(result);
				alert(response.responseMessage);
				if (response.responseCode == 200) {
					$("#susdnoackbtn").prop("disabled", true);
					$('#loader').hide();
				}
			},
			error : function(error) {
				$('#loader').hide();
				console.log(error);
			},
			async : true,
			data : form,
			cache : false,
			contentType : false,
			processData : false,
			timeout : 60000
		});
	});
};

/*
 * ******************************CODE START FOR SUSPENSION RECIPIENT REQUEST ADN
 * RESPONSE******************************
 */

if (barringReqbtnSubmit != null) {
	barringReqbtnSubmit.addEventListener("click", function() {

		var jsonDatas = {
			"billingUid" : billingUID1.value,
			"accountId" : accountID.value,
			"msisdn" : msisdn.value,
			"billDate" : billDate.value,
			"dueDate" : billDueDate.value,
			"amount" : amount.value,
			"comment" : comment.value,
			"reason" : ""
		}

		var form = new FormData();
		form.append("suspension", JSON.stringify(jsonDatas));
		$('#loader').show();
		$.ajax({
			type : "POST",
			url : "api/suspension-rno-request",
			success : function(result) {
				var response = JSON.parse(result);
				alert(response.responseMessage);
				if (response.responseCode == 200) {
					$("#barringReqbtnSubmit").prop("disabled", true);
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
	})
};

/* START CODE FOR BARRING CANCEL */
if (barringcancelbtn != null) {

	barringcancelbtn.addEventListener("click", function() {
		var reason = document.getElementById("reason")
		var transactionId = document.getElementById("transactionId");
		var jsonDatas = {
			"requestId" : transactionId.value,
			"msisdn" : msisdn.value,
			"reason" : reason.value
		}
		var form = new FormData();
		form.append("suspension", JSON.stringify(jsonDatas));
		$('#loader').show();
		$.ajax({
			type : "POST",
			url : "api/suspension-rno-cancel",
			success : function(result) {
				var response = JSON.parse(result);
				alert(response.responseMessage);
				if (response.responseCode == 200) {
					$("#barringcancelbtn").prop("disabled", true);
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
	});
};

/* START CODE FOR BARRING CONFIRMATION REQEUST */
if (barringconfirmbtn != null) {

	barringconfirmbtn.addEventListener("click", function() {
		var transactionId = document.getElementById("transactionId");
		var jsonDatas = {
			"requestId" : transactionId.value,
			"billingUid" : billingUID1.value,
			"accountId" : accountID.value,
			"msisdn" : msisdn.value
		}
		var form = new FormData();
		form.append("suspension", JSON.stringify(jsonDatas));
		$('#loader').show();
		$.ajax({
			type : "POST",
			url : "api/suspension-rno-confirmation",
			success : function(result) {
				var response = JSON.parse(result);
				alert(response.responseMessage);
				if (response.responseCode == 200) {
					$("#barringconfirmbtn").prop("disabled", true);
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
	});
};

function susDNORequest() {
    // Hide all error labels initially
    document.getElementById("billingUidlbl").style.visibility = "hidden";
    document.getElementById("accountIdlbl").style.visibility = "hidden";
    document.getElementById("msisdnlbl").style.visibility = "hidden";
    document.getElementById("billdatelbl").style.visibility = "hidden";
    document.getElementById("duedatelbl").style.visibility = "hidden";
    document.getElementById("amountlbl").style.visibility = "hidden";

    var result = true;

    // Validate Billing UID
    if (billingUID1.value.trim() === "") {
        document.getElementById("billingUidtext").textContent = "Billing UID can't be empty";
        document.getElementById("billingUidlbl").style.visibility = "visible";
        result = false;
    } else if (billingUID1.value.length !== 10) {
        document.getElementById("billingUidtext").textContent = "Billing UID should be exactly 10 characters long";
        document.getElementById("billingUidlbl").style.visibility = "visible";
        result = false;
    }

    // Validate Account ID
    if (accountID.value.trim() === "") {
        document.getElementById("accountIdtext").textContent = "Account ID can't be empty";
        document.getElementById("accountIdlbl").style.visibility = "visible";
        result = false;
    } else if (accountID.value.length !== 10) {
        document.getElementById("accountIdtext").textContent = "Account ID should be exactly 10 characters long";
        document.getElementById("accountIdlbl").style.visibility = "visible";
        result = false;
    }

    // Validate MSISDN
    if (msisdn.value.trim() === "") {
        document.getElementById("msisdntext").textContent = "MSISDN can't be empty";
        document.getElementById("msisdnlbl").style.visibility = "visible";
        result = false;
    } else if (!/^\d{10}$/.test(msisdn.value.trim())) {
        document.getElementById("msisdntext").textContent = "MSISDN must be exactly 10 digits";
        document.getElementById("msisdnlbl").style.visibility = "visible";
        result = false;
    }

    // Validate Bill Date
    if (billDate.value.trim() === "") {
        document.getElementById("billdatetext").textContent = "Bill Date can't be empty";
        document.getElementById("billdatelbl").style.visibility = "visible";
        result = false;
    }

    // Validate Bill Due Date
    if (billDueDate.value.trim() === "") {
        document.getElementById("duedatetext").textContent = "Due Date can't be empty";
        document.getElementById("duedatelbl").style.visibility = "visible";
        result = false;
    }

    // Validate Amount
    if (amount.value.trim() === "") {
        document.getElementById("amounttext").textContent = "Amount can't be empty";
        document.getElementById("amountlbl").style.visibility = "visible";
        result = false;
    } else if (!/^\d+$/.test(amount.value.trim())) {
        document.getElementById("amounttext").textContent = "Amount must be only digits";
        document.getElementById("amountlbl").style.visibility = "visible";
        result = false;
    }

    return result;
}
