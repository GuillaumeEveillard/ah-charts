import React from 'react';
import './App.css';
import { Line } from 'react-chartjs-2';
import {ChartDataSets,  ChartScales} from "chart.js";
import { Nav, Navbar, NavbarBrand, NavLink, TabContent, TabPane, NavItem, Row, Col } from 'reactstrap';
import classnames from 'classnames';

import "./resizable.css";

//const url = ""+window.location;
const url = "http://localhost:9898/";

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
    operations: Operation[],
    stock: ItemInStock[]
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

class ItemInStock{
    item: number;
    quantity: number;
    character: string;
    slot: string;


    constructor(item: number, quantity: number, character: string, slot: string) {
        this.item = item;
        this.quantity = quantity;
        this.character = character;
        this.slot = slot;
    }
}

class QuotationRenderer extends React.Component<QuotationRendererProps, QuotationRendererState> {


    constructor(props: QuotationRendererProps) {
        super(props);
        this.state = {operations: [], stock: []}
    }

    componentDidMount() {
        this.fetchData();
    }

    fetchData() {
        fetch(url+"auctions/history/"+this.props.item)
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
        fetch(url+"stock/"+this.props.item)
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
                    stock: data
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


        let quantityByCharacter = this.state.stock.reduce(function(map: Map<string, number>, obj: ItemInStock) {
            let old = map.get(obj.character);
            if(old === undefined) {
                map.set(obj.character, obj.quantity)
            } else {
                map.set(obj.character, obj.quantity+old)
            }
            return map;
        }, new Map());

        let s = Array.from(quantityByCharacter.entries(),([character, quantity]) => <li>{character+": "+quantity}</li>);

        let buysByDate = this.state.operations
            .filter(o => o.type === "BUY")
            .reduce(function(map: Map<string, Operation[]>, op: Operation) {
                let d = op.time.substr(0, 10);
                let old = map.get(d);
                if(old === undefined) {
                    old = [];
                    map.set(d, old);
                }
                old.push(op);

                return map;
            }, new Map());

        let buys = Array.from(buysByDate.entries())
            .sort((a, b) => a[0].localeCompare(b[0]))
            .map(value => <li><div><b>{value[0]}</b></div>{value[1].map(o => <div>{o.quantity+" @ "+o.price+" "+o.player}</div>)}</li>);

        // TODO remove duplication

        let sellsByDate = this.state.operations
            .filter(o => o.type === "SELL")
            .reduce(function(map: Map<string, Operation[]>, op: Operation) {
                let d = op.time.substr(0, 10);
                let old = map.get(d);
                if(old === undefined) {
                    old = [];
                    map.set(d, old);
                }
                old.push(op);

                return map;
            }, new Map());

        let sells = Array.from(sellsByDate.entries())
            .sort((a, b) => a[0].localeCompare(b[0]))
            .map(value => <li><div><b>{value[0]}</b></div>{value[1].map(o => <div>{o.quantity+" @ "+o.price+" "+o.player}</div>)}</li>);

        const data = {
            // labels: this.props.dates,
            datasets: datasets };
        return (<div>
            <h4>{this.props.chartName}</h4>
            <div style={{marginTop: "0em", marginBottom: "0em"}}>{this.props.comment}</div>
            <div className="container-fluid">
                <div className="row">
                    <div className="col-9">
            <Line
                data={data}
                // width={100}
                // height={50}
                options={{ maintainAspectRatio: false, scales: scale }}
            />
                    </div>
                    <div className="col-1">
                        <ul title={"Stock"}>{s}</ul></div>
                    <div className="col-2">
                        <h4>Buys</h4>
                        <ul title={"Buys"}>{buys}</ul>
                        <h4>Sells</h4>
                        <ul title={"Sells"}>{sells}</ul>
                    </div>
                </div></div>
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
                fetch(url+"quotations/"+id)
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

                fetch(url+"quotations/"+id+"/best-average/10")
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
            let itemName = (item == null) ? itemId.toString() : item.allNames();
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

    allNames() : string {
        return this.id+" / "+this.frenchName+" / "+this.englishName
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

interface TabProps {
    items : Map<number, Item>
    profile: string
}

interface TabState {
    wish : WishListItem[] | null;
    readyToBuy : WishListItem[] | null;
    readyToSell : WishListItem[] | null;
}

class Tab extends React.Component<TabProps, TabState> {
    constructor(props: TabProps) {
        super(props);
        this.state = {
            wish: null,
            readyToBuy: null,
            readyToSell: null
        };
    }

    componentDidMount() {
        this.fetchData();
    }

    fetchData() {
        fetch(url+this.props.profile+"/wish")
            .then(res => res.json())
            .then(data => {
                let wish: WishListItem[] = [];
                data.forEach((i: WishListItem) => {
                    wish.push(new WishListItem(i.id, i.comment, i.buyPrice, i.sellPrice));
                });

                this.setState({wish: wish})
            });

        fetch(url+this.props.profile+"/wish/ready-to-buy")
            .then(res => res.json())
            .then(data => {
                let wish: WishListItem[] = [];
                data.forEach((i: WishListItem) => {
                    wish.push(new WishListItem(i.id, i.comment, i.buyPrice, i.sellPrice));
                });

                this.setState({readyToBuy: wish})
            });

        fetch(url+this.props.profile+"/wish/ready-to-sell")
            .then(res => res.json())
            .then(data => {
                let wish: WishListItem[] = [];
                data.forEach((i: WishListItem) => {
                    wish.push(new WishListItem(i.id, i.comment, i.buyPrice, i.sellPrice));
                });

                this.setState({readyToSell: wish})
            });
    }

    render() {
        return (
            <div>
                <h1>Bonnes affaires - Achat</h1>
                {this.state.readyToBuy != null && this.props.items != null && <QuotationList items={this.props.items} wishItems={this.state.readyToBuy}/> }
                <h1>Bonnes affaires - Vente</h1>
                {this.state.readyToSell != null && this.props.items != null && <QuotationList items={this.props.items} wishItems={this.state.readyToSell}/> }
                <h1>List complète</h1>
                {this.state.wish != null && this.props.items != null && <QuotationList items={this.props.items} wishItems={this.state.wish}/> }
            </div>
        );
    }
}


interface AppProps {

}

interface AppState {
    activeTab: string;
    q : Map<string, number>;
    profiles: string[] | null;
    items : Map<number, Item> | null;

}

class App extends React.Component<AppProps, AppState> {

    constructor(props: AppProps) {
        super(props);
        this.toggle = this.toggle.bind(this);
        this.state = {
            activeTab: '1',
            q : new Map<string, number>(),
            profiles : null,
            items : null
        };
    }


    componentDidMount() {
        this.fetchData();
    }

    fetchData() {
        fetch(url+"items")
            .then(res => res.json())
            .then(data => {
                let items = new Map<number, Item>();
                data.forEach((i: Item) => {
                    items.set(i.id, new Item(i.id, i.frenchName, i.englishName));
                });

                this.setState({items: items})
            });

        fetch(url+"profiles")
            .then(res => res.json())
            .then(data => {
                this.setState({profiles: data})
            });
    }

    toggle(tab: string) {
        if (this.state.activeTab !== tab) {
            this.setState({
                activeTab: tab
            });
        }
    }

    render() {
        let profiles = this.state.profiles;
        let items = this.state.items;
        if (profiles != null && items != null) {


        let tabs = profiles.map(p =>
            <NavItem>
                <NavLink
                    className={classnames({active: this.state.activeTab === p})}
                    onClick={() => {
                        this.toggle(p);
                    }}>
                    {p}
                </NavLink>
            </NavItem>);

        let tabs2 = profiles.map(p =>

            <TabPane tabId={p}>
                <Row>
                    <Col sm="12"><Tab items={items as Map<number, Item>} profile={p}/></Col>
                </Row>
            </TabPane>   );


        return (
            <div>
                <Nav tabs>
                    {tabs}
                </Nav>
                <TabContent activeTab={this.state.activeTab}>
                    {tabs2}
                </TabContent>
            </div>
            // <div className="App">
            //     <ul className="nav nav-tabs">
            //
            //         <li className="nav-item">
            //             <a className="nav-link active" href="#">Active</a>
            //         </li>
            //         <li className="nav-item">
            //             <a className="nav-link" href="#">Link</a>
            //         </li>
            //         <li className="nav-item">
            //             <a className="nav-link" href="#">Link</a>
            //         </li>
            //         <li className="nav-item">
            //             <a className="nav-link disabled" href="#">Disabled</a>
            //         </li>
            //     </ul>
            // </div>
        );
    } else {
            return <div>Loading</div>
        }
    }

}

export default App;
