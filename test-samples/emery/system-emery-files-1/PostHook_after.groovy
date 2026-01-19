Emery.log.error "Pre Hook completed"

def row700 = F.gr5.newRow()
row700.instance_rec_num = 2
row700.api = "Message from Prehook"
row700.results = "Ending the execution with prehook"
F.gr.rows << row700