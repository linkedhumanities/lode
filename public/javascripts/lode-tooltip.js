var toolout = 500;
var tooltimer; 

function lodetooltip() {
	$('a').each(function(index, value) {	
		if($(this).data('toggle') != 'tooltip' && $(this).data('toggle2') != 'tooltip') {
			return;
		}
		
		$(value).unbind();
				
		if($(value).data('tooltip') != undefined) {	
			$(value).tooltip('destroy');
		}
				
		$(value).tooltip({
			html:true,
			trigger:'manual', 
		})
			 .bind('mouseenter', enterTool)
			 .bind('mouseleave', leaveTool);
	});
}
	 
function enterTool() {	
	// clean up
	clearTimeout(tooltimer);
	$('div.tooltip').unbind();
	$('a').not($(this)).tooltip('hide');
	
	// show tooltip
	$(this).tooltip('show');
	
	// initialize tooltip listener
	var vari = $(this);	
	$('div.tooltip').bind('mouseenter', function() {
		clearTimeout(tooltimer);
	});	
	$('div.tooltip').bind('mouseleave', function() {
		$(this).tooltip('hide');
		
		tooltimer = setTimeout(function() {
			vari.tooltip('hide');
		}, toolout);
	});
} 
 
function leaveTool() {
	var vari = $(this);
	
	tooltimer = setTimeout(function() {
		vari.tooltip('hide');
	}, toolout);
}