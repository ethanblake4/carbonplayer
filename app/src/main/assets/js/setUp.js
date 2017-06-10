console.log('loaded setup.js');

var currentField = new inputField("field");

var submitCandidate = new inputField("submit");

function inputField(candidateType) {

    this.candidateType = candidateType;
    this.fieldId = "";
    this.hasId = false;
    this.fieldName = "";
    this.hasName = false;
    this.value = "";

    this.hasForm = false;
    this.inputType = "";
    this.metaType = "";

    this.tagName = "";

    this.formHasPassword = false;
    this.formHasTooManyPasswords = false;

    this.formTextFieldsCount;
    this.hasAction = false;
    this.formAction = "";
    this.formId = "";
    this.formName = "";
    this.hasButton = false;
    this.buttonInner = "";

    this.populateFromElement = function (domElement) {

        var formNode;

        this.fieldId = jQuery(domElement).attr('id');
        console.log(typeof (this.fieldId));

        if (!(typeof (this.fieldId) == "undefined") && (this.fieldId.length > 0)) {
            this.hasId = true;
        } else {
            this.fieldId = '#';

        }

        this.fieldName = jQuery(domElement).attr('name');
        if (this.fieldName !== "undefined") {
            this.hasName = true;
        } else {
            this.fieldName = "#";

        }

        this.value = jQuery(domElement).prop('value');

        formNode = jQuery(domElement).closest("form");

        if (jQuery(formNode).length) {
            console.log('has form');
            this.hasForm = true;
        }

        this.tagName = jQuery(domElement).prop('tagName');

        if (this.tagName == "INPUT") {
            this.inputType = jQuery(domElement).attr('type');

            if ((this.inputType == 'email') || (this.inputType == 'text') || (this.inputType == 'tel') || (typeof (this.inputType) == 'undefined')) {
                this.metaType = "T";

            } else if (this.inputType == 'password') {

                this.metaType = "P";
            } else if (this.inputType == 'submit') {

                this.metaType = "S";
            }

        } else if (this.tagName.toLowerCase() == "button") {

            var buttonType = jQuery(domElement).attr('type');
            this.hasButton = true;
            this.buttonInner = jQuery(domElement).find(':only-child:last').html();
            if (typeof (this.buttonInner) == 'undefined') {
                this.buttonInner = jQuery(domElement).html();

            }
            this.metaType = "B";

        }

        if (this.hasForm) {

            var visibleText = jQuery("input[type=text]:visible,input[type=email]:visible,input:not([type]):visible");

            jQuery(visibleText).each(function () {

               jQuery(this).prop("name");

            });

            this.formTextFieldsCount = jQuery(visibleText).length;

            var visiblePassword = jQuery("input[type=password]:visible");
            passwordCount = jQuery(visiblePassword).length;


            if (passwordCount == 1) {
                this.formHasPassword = true;
            } else if (passwordCount == 0) {
            } else if (passwordCount > 1) {
                this.formHasTooManyPasswords = true;
            }


            if ((this.metaType == "S") || (this.metaType == "B")) {
                this.formAction = jQuery(formNode).prop("action");

                if (typeof (this.formAction) == 'undefined') {

                } else if (this.formAction.length < 1) {
                } else {
                    this.hasAction = true;
                }

            }
            this.formId = jQuery(formNode).prop("id");


            if ((typeof (this.formId) == "undefined") || (jQuery(this.formId).length < 1)) {
                this.formId = "#";
            }

            this.formName = jQuery(formNode).prop("name");

            if ((typeof (this.formName) == "undefined") || (jQuery(this.formName).length < 1)) {

                this.formName = "#";
            }
        }

    };

}



function getLastCurrentField() {
    return JSON.stringify(currentField);
}

function exitWithFieldInfo(someField) {
    var fieldInfoString = JSON.stringify(someField);
    Android.returnResult(fieldInfoString);
}

function armForHasAccount() {
    jQuery("input").click(function () {

        currentField.populateFromElement(jQuery(this));

        if ((currentField.metaType == "T") || (currentField.metaType == "P")) {
            exitWithFieldInfo(currentField); // this should be count of textfields that are close in this form
        } else {}

    });

    return "did arm for has account";
}

function armForNoAccount() {
    jQuery("input").change(function () {

        currentField.populateFromElement(jQuery(this));

        if ((currentField.metaType == "T") || (currentField.metaType == "P")) {
            exitWithFieldInfo(currentField); // this should be count of textfields that are close in this form
        } else {
        }

    });

    return "did arm for no account";
}
jQuery(":button").click(function (event) {

    event.preventDefault();

    submitCandidate.populateFromElement(jQuery(this));

    exitWithFieldInfo(submitCandidate); // this should be count of textfields that are close in this form

});

jQuery("#passwordNext").click(function (event) {
    event.preventDefault();
    submitCandidate.populateFromElement(jQuery(this));
    exitWithFieldInfo(submitCandidate); // this should be count of textfields that are close in this form
});

jQuery(":submit").click(function (event) {

    if(jQuery(this).attr('id') != 'identifierNext') event.preventDefault();

    submitCandidate.populateFromElement(jQuery(this));
    exitWithFieldInfo(submitCandidate); // this should be count of textfields that are close in this form
    // processSubmitOrButtonClick(jQuery(this));

});

function insertValueForAttributeTypeValue(at, av, fieldValue) {

    var selectorType;

    if (at == "N") {

        selectorType = "name";
    } else if (at == "I") {
        selectorType = "id";

    }

    var selector = "[" + selectorType + "=" + av + "]";

    jQuery(selector).val(fieldValue);
}


(function () {
    return "did perform setup";
})();