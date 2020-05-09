import React from 'react';
import './App.css';
import { Line } from 'react-chartjs-2';
import {ChartDataSets,  ChartScales} from "chart.js";
import GridLayout from 'react-grid-layout';

import "./resizable.css";

class Quotation {
    name: string;
    values: Map<string, number>;

    constructor(name: string, values: Map<string, number>) {
        this.name = name;
        this.values = values;
    }
}

interface QuotationRendererProps {
    item: number
    chartName: string
    comment: string | null
    quotations: Quotation[]
    buyPrice: number | null
    sellPrice: number | null
}

interface QuotationRendererState {
    operations: Operation[]
}

class Operation {
     type: string;
     item: number;
     stackSize: number;
     quantity: number;
     price: number;
     otherPlayer: string;
     player: string;
     time: string;

    constructor(type: string, item: number, stackSize: number, quantity: number, price: number, otherPlayer: string, player: string, time: string) {
        this.type = type;
        this.item = item;
        this.stackSize = stackSize;
        this.quantity = quantity;
        this.price = price;
        this.otherPlayer = otherPlayer;
        this.player = player;
        this.time = time;
    }
}

class QuotationRenderer extends React.Component<QuotationRendererProps, QuotationRendererState> {


    constructor(props: QuotationRendererProps) {
        super(props);
        this.state = {operations: []}
    }

    componentDidMount() {
        this.fetchData();
    }

    fetchData() {
        fetch("http://localhost:9898/auctions/history/"+this.props.item)
            .then(res => {
                console.log(res);
                return res;
            })
            .then(res => res.json())
            .then(res => {
                console.log(res);
                return res;
            })
            .then(data => {
                this.setState({
                    operations: data
                })
            })
            .catch(function (error) {
                console.error(error);
                return error;
            });
    }

    render() {
        let dates = this.props.quotations.map(q => Array.from(q.values.keys())).flat().sort();
        let startDate = dates[0];
        let endDate = dates[dates.length-1];

        const datasets : ChartDataSets[] = this.props.quotations.map(q => ({
            label: q.name,
            borderColor: 'rgb(0, 0, 255)',
            data: Array.from(q.values.entries()).map(e => ({x: e[0], y: e[1]}))
        }));
        if(this.props.buyPrice != null) {
            datasets.push({
                label: "Buy price",
                borderColor: 'rgb(0, 255, 0)',
                data: [{
                    x: startDate,
                    y: this.props.buyPrice
                }, {x: endDate, y: this.props.buyPrice}]
            });
        }
        if(this.props.sellPrice != null) {
            datasets.push({
                label: "Sell price",
                borderColor: 'rgb(255, 0, 0)',
                data: [{
                    x: startDate,
                    y: this.props.sellPrice
                }, {x: endDate, y: this.props.sellPrice}]
            });
        }

        let scale: ChartScales = {
            xAxes: [{type: "time", distribution: "linear", time: {unit: "day", displayFormats: {hour: "MMM D hA"}}}]
        };

        let op = this.state.operations.map(o => <li>{o.type+" Q: "+o.quantity+" | P: "+o.price+" | "+o.player+" | "+o.time}</li>)

        const data = {
            // labels: this.props.dates,
            datasets: datasets };
        return (<div>
            <h4>{this.props.chartName}</h4>
            <div style={{marginTop: "0em", marginBottom: "0em"}}>{this.props.comment}</div>
            <Line
                data={data}
                // width={100}
                // height={50}
                options={{ maintainAspectRatio: false, scales: scale }}
            />
            <ul>{op}</ul>
        </div>);
    }
}

interface QuotationListProps {
    wishItems: WishListItem[],
    items: Map<number, Item>
}

interface QuotationListState {
    itemNames: Map<number, string>
    // item id => (time, price)
    quotations: Map<number, Map<string, number>>
    quotationsBestAverage: Map<number, Map<string, number>>
}

class QuotationList extends React.Component<QuotationListProps, QuotationListState> {
    constructor(props: QuotationListProps) {
        super(props);
        this.state = {
            itemNames : new Map<number, string>(),
            quotations : new Map<number, Map<string, number>>(),
            quotationsBestAverage : new Map<number, Map<string, number>>()
        };
    }

    componentDidMount() {
        this.fetchData();
    }


    fetchData() {
        this.props.wishItems
            .map(i => i.id)
            .forEach(id => {
                fetch("http://localhost:9898/quotations/"+id)
                .then(res => {
                    console.log(res);
                    return res;
                })
                .then(res => res.json())
                .then(res => {
                    console.log(res);
                    return res;
                })
                .then(data => {
                    let q = new Map<string, number>();
                    Object.keys(data).forEach(i => {
                        q.set(i, data[i]);
                    });
                    return q;
                })
                .then(data => {
                    let q = new Map(this.state.quotations);
                    q.set(id, data); //side effect
                    this.setState({quotations: q})
                })
                .catch(function (error) {
                    console.error(error);
                    return error;
                });

                fetch("http://localhost:9898/quotations/"+id+"/best-average/10")
                    .then(res => {
                        console.log(res);
                        return res;
                    })
                    .then(res => res.json())
                    .then(res => {
                        console.log(res);
                        return res;
                    })
                    .then(data => {
                        let q = new Map<string, number>();
                        Object.keys(data).forEach(i => {
                            q.set(i, data[i]);
                        });
                        return q;
                    })
                    .then(data => {
                        let q = new Map(this.state.quotationsBestAverage);
                        q.set(id, data); //side effect
                        this.setState({quotationsBestAverage: q})
                    })
                    .catch(function (error) {
                        console.error(error);
                        return error;
                    });
        });
    }

    render() {

        let charts = Array.from(this.props.wishItems.values()).map((wishItem, index) => {
            let itemId = wishItem.id;
            let item = this.props.items.get(wishItem.id);
            let itemName = (item == null) ? itemId.toString() : item.frenchNameIfPossible();
            let quotation = this.state.quotations.get(wishItem.id);
            let quotationBestAverage = this.state.quotationsBestAverage.get(wishItem.id);

            if(quotation == null || quotationBestAverage == null) {
                return (<div key={"elem-"+index} data-grid={{x: index%2*2, y: index/2, w: 2, h: 1}}>No quotation</div>);
            } else {
                return (
                    <div key={"elem-"+index} data-grid={{x: index%2*2, y: index/2, w: 2, h: 1, isResizable: true}} style={{
                        color: "black",
                        height: "auto",
                        width: "auto",
                        background: "white",
                        overflow: "hidden"
                    }}>
                    <QuotationRenderer
                        item={itemId}
                    chartName={itemName}
                    comment={wishItem.comment}
                    buyPrice={wishItem.buyPrice}
                    sellPrice={wishItem.sellPrice}
                    quotations={[
                        new Quotation(itemName, quotation), new Quotation(itemName+" (avg 10)", quotationBestAverage)
                    ]}
                />
                    </div>)
            }
        });

        // return (<div style={{ background: "grey" }}><GridLayout className="layout" cols={4} rowHeight={400} width={1800}>{charts}</GridLayout></div>);
        return (<div>{charts}</div>);
    }

}

class Item {
    id: number;
    frenchName: string | null;
    englishName: string | null;

    constructor(id: number, frenchName: string | null, englishName: string | null) {
        this.id = id;
        this.frenchName = frenchName;
        this.englishName = englishName;
    }

    frenchNameIfPossible(): string {
        if(this.frenchName != null) return this.frenchName;
        if(this.englishName != null) return this.englishName;
        return this.id.toString();
    }
}

class WishListItem {
    id: number;
    comment: string | null;
    buyPrice: number | null;
    sellPrice: number | null;


    constructor(id: number, comment: string | null, buyPrice: number | null, sellPrice: number | null) {
        this.id = id;
        this.comment = comment;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }
}

// class MyFirstGrid extends React.Component {
//     render() {
//         return (
//             <GridLayout className="layout" cols={8} rowHeight={30} width={1200}>
//                 <div key="a" data-grid={{x: 0, y: 0, w: 1, h: 2, static: true}}>a</div>
//                 <div key="b" data-grid={{x: 1, y: 0, w: 3, h: 2, minW: 2, maxW: 4}}>b</div>
//                 <div key="c" data-grid={{x: 4, y: 0, w: 1, h: 2}}>c</div>
//             </GridLayout>
//         )
//     }
// }


interface AppProps {

}

interface AppState {
    q : Map<string, number>;
    items : Map<number, Item> | null;
    wish : WishListItem[] | null;
    readyToBuy : WishListItem[] | null;
}

class App extends React.Component<AppProps, AppState> {

    constructor(props: AppProps) {
        super(props);
        this.state = {
            q : new Map<string, number>(),
            items : null,
            wish: null,
            readyToBuy: null
        };
    }


    componentDidMount() {
        this.fetchData();
    }

    fetchData() {
        fetch("http://localhost:9898/items")
            .then(res => res.json())
            .then(data => {
                let items = new Map<number, Item>();
                data.forEach((i: Item) => {
                    items.set(i.id, new Item(i.id, i.frenchName, i.englishName));
                });

                this.setState({items: items})
            });

        fetch("http://localhost:9898/wish")
            .then(res => res.json())
            .then(data => {
                let wish: WishListItem[] = [];
                data.forEach((i: WishListItem) => {
                    wish.push(new WishListItem(i.id, i.comment, i.buyPrice, i.sellPrice));
                });

                this.setState({wish: wish})
            });

        fetch("http://localhost:9898/wish/ready-to-buy")
            .then(res => res.json())
            .then(data => {
                let wish: WishListItem[] = [];
                data.forEach((i: WishListItem) => {
                    wish.push(new WishListItem(i.id, i.comment, i.buyPrice, i.sellPrice));
                });

                this.setState({readyToBuy: wish})
            });
    }
        render()
        {
            return (
                <div className="App">
                    <h1>Bonnes affaires</h1>
                    {this.state.readyToBuy != null && this.state.items != null && <QuotationList items={this.state.items} wishItems={this.state.readyToBuy}/> }
                    <h1>List complète</h1>
                    {this.state.wish != null && this.state.items != null && <QuotationList items={this.state.items} wishItems={this.state.wish}/> }
                </div>
            );
        }

}

export default App;
