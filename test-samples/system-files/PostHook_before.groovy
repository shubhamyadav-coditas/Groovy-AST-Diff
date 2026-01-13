Emery.log.error "Post Hook Started"

def row600 = F.gr5.newRow()
row600.instance_rec_num = 1
row600.api = "Message from Posthook"
row600.results = "Ending the execution with posthook"
F.gr.rows << row600